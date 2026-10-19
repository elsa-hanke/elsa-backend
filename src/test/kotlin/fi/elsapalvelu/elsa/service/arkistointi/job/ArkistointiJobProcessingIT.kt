package fi.elsapalvelu.elsa.service.arkistointi.job

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerName
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.CompletionHandler
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.FailureHandler
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.mockito.kotlin.verifyNoInteractions
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import java.io.ByteArrayOutputStream
import java.net.URI
import java.sql.Connection
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.cache.Caching
import javax.sql.DataSource

/** All SQL writes, including simulated crashes, target this isolated disposable test database. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///archiveProcessing?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "db-scheduler.enabled=false",
    "application.arkistointi.retry.initial-delay=1s",
    "application.arkistointi.retry.max-delay=2s",
    "application.arkistointi.retry.max-attempts=3"
])
@Import(ArkistointiJobProcessingIT.TestConfig::class)
class ArkistointiJobProcessingIT {
    @Autowired private lateinit var creator: ArkistointiJobCreator
    @Autowired private lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var pdfA: PdfA2bService
    @Autowired private lateinit var adapter: TestAdapter
    @Autowired private lateinit var task: OneTimeTask<Unit>
    @Autowired private lateinit var dataSource: DataSource
    @Autowired private lateinit var processor: ArkistointiJobProcessor
    @Autowired private lateinit var failureHandler: ArkistointiJobFailureHandler
    @Autowired private lateinit var state: ArkistointiJobStateService
    @MockitoSpyBean private lateinit var loader: ArkistointiAsiakirjaLoader
    private val schedulers = mutableListOf<Scheduler>()

    @BeforeEach
    fun resetAdapter() {
        adapter.submissions.clear()
        adapter.behavior = { ArkistointiResult.Success("external-id") }
    }

    @AfterEach
    fun stopSchedulers() {
        schedulers.forEach(Scheduler::stop)
        transactions.executeWithoutResult {
            jdbc.update("delete from scheduled_tasks where task_name = ?", ARKISTOINTI_JOB_TASK.taskName)
        }
    }

    @Test
    fun `retry is delayed and success persists metadata before removing scheduler row`() {
        val jobId = fixture()
        val originalExecution = execution(jobId)
        adapter.behavior = {
            if (adapter.submissions.size == 1) retryable() else ArkistointiResult.Success("external-id")
        }
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(failures(jobId)).isEqualTo(1) }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
        val nextExecution = jdbc.queryForObject(
            "select execution_time from scheduled_tasks where task_instance = ?", java.sql.Timestamp::class.java, jobId.toString()
        )!!.toInstant()
        assertThat(nextExecution).isAfter(Instant.now())
        runUntil(scheduler) { assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY) }
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        val stored = jobs.findById(jobId).orElseThrow()
        assertThat(stored.externalId).isEqualTo("external-id")
        assertThat(stored.completedAt).isNotNull()
        assertThat(stored.errorCode).isNull()
        assertThat(stored.lastError).isNull()
        assertThat(adapter.submissions).hasSize(2)
        assertThat(adapter.submissions.map { it.key }.distinct()).hasSize(1)
        assertThat(adapter.submissions.map { it.payload }).containsOnly("{\"approved\":true}")
        assertThat(adapter.submissions.first().asiakirjat.single().sisalto)
            .containsExactly(*adapter.submissions.last().asiakirjat.single().sisalto)
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        assertThat(adapter.submissions).hasSize(2)
    }

    @Test
    fun `retry limit includes the first delivery`() {
        val jobId = fixture()
        val originalExecution = execution(jobId)
        adapter.behavior = { retryable() }
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA) }
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(adapter.submissions).hasSize(3)
        assertThat(jobs.findById(jobId).orElseThrow().errorCode).isEqualTo("ARCHIVE_RETRIES_EXHAUSTED")
        assertThat(jobs.findById(jobId).orElseThrow().lastError).contains("UNAVAILABLE", "Palvelu ei vastaa")
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        assertThat(adapter.submissions).hasSize(3)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 2, 3])
    fun `permanent adapter failure terminates without retry`(previousFailures: Int) {
        val jobId = fixture()
        transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set consecutive_failures = ? where task_instance = ?", previousFailures, jobId.toString())
        }
        adapter.behavior = { ArkistointiResult.PermanentFailure(ArkistointiError.create("REJECTED", "Aineisto hylattiin")) }
        assertTerminal(jobId, "REJECTED")
        assertThat(adapter.submissions).hasSize(1)
    }

    @ParameterizedTest
    @ValueSource(strings = ["NULL", "MAX_ASCII", "MAX_UNICODE", "OPAQUE", "TOO_LONG", "TOO_LONG_UNICODE",
        "EMPTY", "BLANK", "NUL", "HIGH_SURROGATE", "LOW_SURROGATE"])
    fun `external ID contract controls whether success or manual intervention is committed`(kind: String) {
        val externalId = ArkistointiExternalIdTestData.value(kind)
        val valid = kind in setOf("NULL", "MAX_ASCII", "MAX_UNICODE", "OPAQUE")
        adapter.behavior = { ArkistointiResult.Success(externalId) }
        val jobId = fixture()
        val originalExecution = execution(jobId)
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        val stored = jobs.findById(jobId).orElseThrow()
        if (valid) {
            assertThat(stored.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
            assertThat(stored.externalId).isEqualTo(externalId)
            assertThat(stored.completedAt).isNotNull()
            assertThat(stored.errorCode).isNull()
            assertThat(stored.lastError).isNull()
        } else {
            assertThat(stored.status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
            assertThat(stored.externalId).isNull()
            assertThat(stored.completedAt).isNull()
            assertThat(stored.errorCode).isEqualTo("ARCHIVE_ADAPTER_RESPONSE_INVALID")
            assertThat(stored.lastError).doesNotContain("private-response")
        }
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        scheduler.triggerCheckForDueExecutions()
        assertThat(adapter.submissions).hasSize(1)
    }

    @ParameterizedTest
    @CsvSource("PERMANENT,false", "INVALID_SUCCESS,false", "PERMANENT,true", "INVALID_SUCCESS,true")
    fun `scheduler recovers failed terminal commit with the same key and bounded attempts`(result: String, exhaust: Boolean) {
        val expectedCode = if (result == "PERMANENT") "OLD_ERROR" else "ARCHIVE_ADAPTER_RESPONSE_INVALID"
        val jobId = fixture()
        val originalExecution = execution(jobId)
        val originalKey = jobs.findById(jobId).orElseThrow().key
        val attemptCounters = CopyOnWriteArrayList<Int>()
        adapter.behavior = {
            attemptCounters.add(failures(jobId))
            if (exhaust && adapter.submissions.size > 1) retryable() else lateResult(result)
        }
        val failedCommit = CountDownLatch(1)
        val failure = AtomicReference<Throwable>()
        val observingHandler = FailureHandler<Unit> { current, executionOperations ->
            try {
                failureHandler.onFailure(current, executionOperations)
            } catch (exception: Exception) {
                failure.set(exception)
                failedCommit.countDown()
                throw exception
            }
        }
        installFailingTerminalCommit()
        val observedTask = object : OneTimeTask<Unit>(ARKISTOINTI_JOB_TASK.taskName, Unit::class.java, observingHandler, task.deadExecutionHandler) {
            override fun executeOnce(instance: TaskInstance<Unit>, context: ExecutionContext) = task.executeOnce(instance, context)
        }
        val scheduler = scheduler(observedTask, Duration.ofHours(1))
        try {
            scheduler.triggerCheckForDueExecutions()
            assertThat(failedCommit.await(10, TimeUnit.SECONDS)).isTrue()
            await().atMost(Duration.ofSeconds(10)).untilAsserted { assertThat(scheduler.currentlyExecuting).isEmpty() }
            val exception = failure.get() as ArkistointiProcessingException
            assertThat(exception.retryable).isFalse()
            assertThat(exception.error.code).isEqualTo(expectedCode)
            assertThat(exception.cause).isNull()
            assertUncommittedTerminalState(jobId)
        } finally {
            scheduler.stop()
            transactions.executeWithoutResult {
                jdbc.execute("DROP TRIGGER IF EXISTS fail_archive_terminal ON arkistointi_job")
                jdbc.execute("DROP FUNCTION IF EXISTS fail_archive_terminal()")
            }
        }
        // Recovery must receive a fresh event from db-scheduler, without the uncommitted outcome.
        val recovered = CopyOnWriteArrayList<ExecutionComplete>()
        transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set last_heartbeat = ? where task_instance = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), jobId.toString())
        }
        val recoveryScheduler = scheduler(observingRecoveryTask(recovered))
        runUntil(recoveryScheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(recovered).hasSize(1)
        assertThat(recovered.single().cause).isEmpty()
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
        assertThat(jobs.findById(jobId).orElseThrow().errorCode)
            .isEqualTo(if (exhaust) "ARCHIVE_RETRIES_EXHAUSTED" else expectedCode)
        assertThat(attemptCounters).containsExactlyElementsOf(if (exhaust) listOf(0, 1, 2) else listOf(0, 1))
        assertThat(adapter.submissions.map { it.key }).containsOnly(originalKey)
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        assertThat(adapter.submissions).hasSize(if (exhaust) 3 else 2)
    }

    private fun observingRecoveryTask(recovered: MutableList<ExecutionComplete>): OneTimeTask<Unit> =
        object : OneTimeTask<Unit>(ARKISTOINTI_JOB_TASK.taskName, Unit::class.java, failureHandler,
            { current, operations ->
                recovered.add(current)
                task.deadExecutionHandler.deadExecution(current, operations)
            }) {
            override fun executeOnce(instance: TaskInstance<Unit>, context: ExecutionContext) = task.executeOnce(instance, context)
        }

    private fun assertUncommittedTerminalState(jobId: Long) {
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.KASITTELYSSA)
        assertThat(jobs.findById(jobId).orElseThrow().errorCode).isNull()
        assertThat(schedulerState(jobId)).containsEntry("picked", true)
        assertThat(failures(jobId)).isZero()
    }

    private fun installFailingTerminalCommit() {
        transactions.executeWithoutResult {
            jdbc.execute("""
                CREATE FUNCTION fail_archive_terminal() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                BEGIN
                    IF NEW.status = 'VAATII_TOIMENPITEITA' THEN RAISE EXCEPTION 'Test terminal commit failure'; END IF;
                    RETURN NEW;
                END; ${'$'}${'$'}
            """.trimIndent())
            // Deferred constraint trigger: the UPDATE/flush succeeds and COMMIT itself fails.
            jdbc.execute("CREATE CONSTRAINT TRIGGER fail_archive_terminal AFTER UPDATE ON arkistointi_job " +
                "DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fail_archive_terminal()")
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `state success validates response defensively after checking execution ownership`(stale: Boolean) {
        val jobId = fixture()
        simulateCrash(jobId)
        val originalExecution = execution(jobId)
        if (stale) transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set version = version + 1 where task_instance = ?", jobId.toString())
        }
        val beforeJob = jobState(jobId)
        val beforeSchedule = schedulerState(jobId)
        if (stale) {
            assertThrows<StaleArkistointiExecutionException> { state.succeed(originalExecution, "x".repeat(256)) }
        } else {
            val exception = assertThrows<ArkistointiProcessingException> { state.succeed(originalExecution, "x".repeat(256)) }
            assertThat(exception.error.code).isEqualTo("ARCHIVE_ADAPTER_RESPONSE_INVALID")
            assertThat(exception.retryable).isFalse()
        }
        assertThat(jobState(jobId)).isEqualTo(beforeJob)
        assertThat(schedulerState(jobId)).isEqualTo(beforeSchedule)
        assertThat(adapter.submissions).isEmpty()
    }

    @Test
    fun `unexpected exceptions are sanitized and retried`() {
        val jobId = fixture()
        adapter.behavior = { throw IllegalStateException("secret personal response body") }
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(failures(jobId)).isEqualTo(1) }
        val stored = jobs.findById(jobId).orElseThrow()
        assertThat(stored.errorCode).isEqualTo("ARCHIVE_PROCESSING_FAILED")
        assertThat(stored.lastError).doesNotContain("secret")
        adapter.behavior = { ArkistointiResult.Success(null) }
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
    }

    @Test
    fun `missing adapter terminates with an explicit error`() {
        val jobId = fixture(YliopistoEnum.HELSINGIN_YLIOPISTO)
        assertTerminal(jobId, "ARCHIVE_ADAPTER_MISSING")
        assertThat(adapter.submissions).isEmpty()
    }

    @Test
    fun `checksum mismatch and invalid payload terminate without invoking adapter`() {
        val corruptJob = fixture()
        transactions.executeWithoutResult {
            val job = jobs.findById(corruptJob).orElseThrow()
            job.asiakirjat.single().sha256 = "0".repeat(64)
        }
        assertTerminal(corruptJob, "ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH")
        val invalidJob = fixture()
        transactions.executeWithoutResult { jobs.findById(invalidJob).orElseThrow().payload = "not-json" }
        assertTerminal(invalidJob, "ARCHIVE_JOB_INVALID")
        assertThat(adapter.submissions).isEmpty()
    }

    @Test
    fun `dead execution resumes processing with the original idempotency key`() {
        val jobId = fixture()
        simulateCrash(jobId)
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions.single().key).isEqualTo(jobs.findById(jobId).orElseThrow().key)
    }

    @Test
    fun `dead execution after successful commit is acknowledged without another delivery`() {
        val jobId = fixture()
        simulateCrash(jobId)
        processor.process(execution(jobId))
        simulateCrash(jobId, completed = true)
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(1)
    }

    @Test
    fun `remote success followed by a failed local commit retries with the same key`() {
        val jobId = fixture()
        transactions.executeWithoutResult {
            jdbc.execute("""
                CREATE FUNCTION fail_archive_success() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                BEGIN
                    IF NEW.status = 'LAHETETTY' THEN RAISE EXCEPTION 'Test success commit failure'; END IF;
                    RETURN NEW;
                END; ${'$'}${'$'}
            """.trimIndent())
            jdbc.execute("CREATE TRIGGER fail_archive_success BEFORE UPDATE ON arkistointi_job " +
                "FOR EACH ROW EXECUTE FUNCTION fail_archive_success()")
        }
        val scheduler = scheduler()
        try {
            runUntil(scheduler) { assertThat(failures(jobId)).isEqualTo(1) }
            assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
        } finally {
            transactions.executeWithoutResult {
                jdbc.execute("DROP TRIGGER fail_archive_success ON arkistointi_job")
                jdbc.execute("DROP FUNCTION fail_archive_success()")
            }
        }
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(2)
        assertThat(adapter.submissions.map { it.key }.distinct()).hasSize(1)
    }

    @Test
    fun `failed retry reschedule is recovered after job state was committed`() {
        val jobId = fixture()
        val failOnce = AtomicBoolean(true)
        adapter.behavior = {
            if (failOnce.getAndSet(false)) {
                transactions.executeWithoutResult {
                    jdbc.execute("""
                        CREATE FUNCTION fail_archive_reschedule() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                        BEGIN
                            IF OLD.picked AND NOT NEW.picked THEN RAISE EXCEPTION 'Test reschedule failure'; END IF;
                            RETURN NEW;
                        END; ${'$'}${'$'}
                    """.trimIndent())
                    jdbc.execute("CREATE TRIGGER fail_archive_reschedule BEFORE UPDATE ON scheduled_tasks " +
                        "FOR EACH ROW EXECUTE FUNCTION fail_archive_reschedule()")
                }
                retryable()
            } else ArkistointiResult.Success(null)
        }
        val scheduler = scheduler()
        try {
            runUntil(scheduler) {
                assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
                assertThat(jdbc.queryForObject(
                    "select picked from scheduled_tasks where task_instance = ?", Boolean::class.javaObjectType, jobId.toString()
                )).isTrue()
                assertThat(scheduler.currentlyExecuting).isEmpty()
            }
        } finally {
            transactions.executeWithoutResult {
                jdbc.execute("DROP TRIGGER IF EXISTS fail_archive_reschedule ON scheduled_tasks")
                jdbc.execute("DROP FUNCTION IF EXISTS fail_archive_reschedule()")
            }
        }
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(2)
    }

    @Test
    fun `two scheduler instances do not deliver the same active task concurrently`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        adapter.behavior = {
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
            ArkistointiResult.Success(null)
        }
        val jobId = fixture()
        val first = scheduler()
        val second = scheduler()
        try {
            first.triggerCheckForDueExecutions()
            second.triggerCheckForDueExecutions()
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue()
            await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(3)).untilAsserted {
                second.triggerCheckForDueExecutions()
                assertThat(adapter.submissions).hasSize(1)
            }
        } finally {
            release.countDown()
        }
        runUntil(first) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(1)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `stale recovery handler cannot change a newer running or successful job`(afterSuccess: Boolean) {
        val jobId = fixture()
        simulateCrash(jobId)
        val recovery = RecoveryGate()
        val deliveryEntered = CountDownLatch(1)
        val releaseDelivery = CountDownLatch(1)
        val recoveryTask = recovery.task(task, failureHandler)
        adapter.behavior = {
            deliveryEntered.countDown()
            check(releaseDelivery.await(10, TimeUnit.SECONDS))
            ArkistointiResult.Success("external-id")
        }
        val first = scheduler(recoveryTask)
        scheduler(recoveryTask)
        try {
            runUntil(first) { assertThat(deliveryEntered.count).isZero() }
            assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.KASITTELYSSA)
            if (afterSuccess) {
                releaseDelivery.countDown()
                runUntil(first) { assertThat(taskCount(jobId)).isZero() }
            }
            recovery.releaseStale.countDown()
            assertThat(recovery.staleFinished.await(10, TimeUnit.SECONDS)).isTrue()
            assertThat(recovery.staleError.get()).isNull()
            assertThat(recovery.versions).hasSize(2)
            assertThat(recovery.versions.distinct()).hasSize(1)
            assertThat(status(jobId)).isEqualTo(
                if (afterSuccess) ArkistointiJobStatus.LAHETETTY else ArkistointiJobStatus.KASITTELYSSA
            )
        } finally {
            recovery.releaseStale.countDown()
            releaseDelivery.countDown()
        }
        runUntil(first) { assertThat(taskCount(jobId)).isZero() }
        val stored = jobs.findById(jobId).orElseThrow()
        assertThat(stored.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(stored.externalId).isEqualTo("external-id")
        assertThat(stored.completedAt).isNotNull()
        assertThat(stored.errorCode).isNull()
        assertThat(stored.lastError).isNull()
        assertThat(adapter.submissions).hasSize(1)
    }

    @Test
    fun `concurrent state writer rechecks terminal status after waiting for a row lock`() {
        val jobId = fixture()
        transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set picked = true where task_instance = ?", jobId.toString())
        }
        val currentExecution = execution(jobId)
        val executor = Executors.newSingleThreadExecutor()
        lateinit var lateFailure: Future<Boolean>
        try {
            transactions.executeWithoutResult {
                val job = jobs.findByIdForStateChange(jobId)!!
                val lockOwner = jdbc.queryForObject("select pg_backend_pid()", Int::class.javaObjectType)!!
                lateFailure = executor.submit<Boolean> {
                    state.fail(currentExecution, ArkistointiError.create("LATE_ERROR", "Viivastynyt virhe"), false)
                }
                // Wait for an actual database lock wait, rather than relying on thread timing.
                await().atMost(Duration.ofSeconds(10)).until {
                    jdbc.queryForObject(
                        "select count(*) from pg_locks where not granted and ? = any(pg_blocking_pids(pid))",
                        Long::class.javaObjectType, lockOwner
                    )!! > 0
                }
                job.transitionTo(ArkistointiJobStatus.KASITTELYSSA)
                job.transitionTo(ArkistointiJobStatus.LAHETETTY)
                job.externalId = "committed-success"
                job.completedAt = Instant.now()
            }
            assertThat(lateFailure.get(10, TimeUnit.SECONDS)).isFalse()
            val stored = jobs.findById(jobId).orElseThrow()
            assertThat(stored.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
            assertThat(stored.externalId).isEqualTo("committed-success")
            assertThat(stored.completedAt).isNotNull()
            assertThat(stored.errorCode).isNull()
            assertThat(stored.lastError).isNull()
        } finally {
            executor.shutdownNow()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["UNPICKED", "REPLACED", "ACKNOWLEDGED"])
    fun `stale prepare never loads or sends documents`(reason: String) {
        val jobId = fixture()
        val originalExecution = execution(jobId)
        transactions.executeWithoutResult {
            when (reason) {
                "REPLACED" -> jdbc.update("update scheduled_tasks set picked = true, version = version + 1 where task_instance = ?", jobId.toString())
                "ACKNOWLEDGED" -> jdbc.update("delete from scheduled_tasks where task_instance = ?", jobId.toString())
            }
        }
        val stored = schedulerStateOrEmpty(jobId)
        assertThrows<StaleArkistointiExecutionException> { processor.process(originalExecution) }
        verifyNoInteractions(loader)
        assertThat(adapter.submissions).isEmpty()
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
        assertThat(schedulerStateOrEmpty(jobId)).isEqualTo(stored)
    }

    private fun execution(jobId: Long) = ArkistointiJobExecution(
        ARKISTOINTI_JOB_TASK.taskName, jobId.toString(),
        jdbc.queryForObject("select version from scheduled_tasks where task_instance = ?", Long::class.javaObjectType, jobId.toString())!!
    )

    private fun schedulerStateOrEmpty(jobId: Long) = if (taskCount(jobId) == 0L) emptyMap() else schedulerState(jobId)

    @ParameterizedTest
    @CsvSource("PERMANENT,false", "SUCCESS,false", "INVALID_SUCCESS,false", "RETRYABLE,false", "EXCEPTION,false",
        "PERMANENT,true", "SUCCESS,true", "INVALID_SUCCESS,true", "RETRYABLE,true", "EXCEPTION,true")
    fun `late adapter result cannot overwrite a newer execution`(result: String, afterSuccess: Boolean) {
        val firstEntered = CountDownLatch(1)
        val secondEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val releaseSecond = CountDownLatch(1)
        val calls = AtomicInteger()
        val versions = CopyOnWriteArrayList<Long>()
        adapter.behavior = {
            if (calls.getAndIncrement() == 0) {
                firstEntered.countDown()
                check(releaseFirst.await(20, TimeUnit.SECONDS))
                lateResult(result)
            } else {
                secondEntered.countDown()
                check(releaseSecond.await(20, TimeUnit.SECONDS))
                ArkistointiResult.Success("new-external-id")
            }
        }
        val jobId = fixture()
        val first = scheduler(observedTask(versions), Duration.ofHours(1))
        try {
            assertThat(firstEntered.await(10, TimeUnit.SECONDS)).isTrue()
            // Lose the heartbeat without stopping or interrupting the original adapter call.
            transactions.executeWithoutResult {
                jdbc.update("update scheduled_tasks set last_heartbeat = ? where task_instance = ?",
                    java.sql.Timestamp.from(Instant.now().minusSeconds(60)), jobId.toString())
            }
            val second = scheduler(observedTask(versions))
            runUntil(second) { assertThat(secondEntered.count).isZero() }
            assertThat(versions).hasSize(2)
            assertThat(versions.last()).isGreaterThan(versions.first())
            val current = schedulerState(jobId)
            val currentJob = jobState(jobId)
            if (afterSuccess) {
                releaseSecond.countDown()
                runUntil(second) { assertThat(taskCount(jobId)).isZero() }
            }
            releaseFirst.countDown()
            await().atMost(Duration.ofSeconds(10)).untilAsserted { assertThat(first.currentlyExecuting).isEmpty() }
            assertThat(status(jobId)).isEqualTo(
                if (afterSuccess) ArkistointiJobStatus.LAHETETTY else ArkistointiJobStatus.KASITTELYSSA
            )
            if (!afterSuccess) {
                assertThat(schedulerState(jobId)).isEqualTo(current)
                assertThat(jobState(jobId)).isEqualTo(currentJob)
            }
            releaseSecond.countDown()
            runUntil(second) { assertThat(taskCount(jobId)).isZero() }
            assertNewSuccess(jobId)
        } finally {
            releaseFirst.countDown()
            releaseSecond.countDown()
        }
    }

    private fun lateResult(result: String): ArkistointiResult = when (result) {
        "PERMANENT" -> ArkistointiResult.PermanentFailure(ArkistointiError.create("OLD_ERROR", "Vanha virhe"))
        "SUCCESS" -> ArkistointiResult.Success("old-external-id")
        "INVALID_SUCCESS" -> ArkistointiResult.Success("x".repeat(256))
        "RETRYABLE" -> retryable()
        else -> error("secret old response")
    }

    private fun schedulerState(jobId: Long) = jdbc.queryForMap(
        "select version, picked, execution_time, consecutive_failures from scheduled_tasks where task_instance = ?",
        jobId.toString()
    )

    private fun jobState(jobId: Long) = jobs.findById(jobId).orElseThrow().let {
        listOf(it.status, it.externalId, it.completedAt, it.errorCode, it.lastError)
    }

    private fun assertNewSuccess(jobId: Long) {
        val stored = jobs.findById(jobId).orElseThrow()
        assertThat(stored.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(stored.externalId).isEqualTo("new-external-id")
        assertThat(stored.completedAt).isNotNull()
        assertThat(stored.errorCode).isNull()
        assertThat(stored.lastError).isNull()
        assertThat(adapter.submissions).hasSize(2)
        assertThat(adapter.submissions.map { it.key }.distinct()).hasSize(1)
    }

    private fun observedTask(versions: MutableList<Long>): OneTimeTask<Unit> =
        object : OneTimeTask<Unit>(ARKISTOINTI_JOB_TASK.taskName, Unit::class.java,
            failureHandler, task.deadExecutionHandler) {
            override fun executeOnce(instance: TaskInstance<Unit>, context: ExecutionContext) =
                task.executeOnce(instance, context)

            override fun execute(instance: TaskInstance<Unit>, context: ExecutionContext): CompletionHandler<Unit> {
                versions.add(context.execution.version)
                return task.execute(instance, context)
            }
        }

    private class RecoveryGate {
        private val secondDetected = CountDownLatch(1)
        private val recovered = AtomicInteger()
        val releaseStale = CountDownLatch(1)
        val staleFinished = CountDownLatch(1)
        val staleError = AtomicReference<Throwable>()
        val versions = CopyOnWriteArrayList<Long>()

        fun task(delegate: OneTimeTask<Unit>, handler: ArkistointiJobFailureHandler): OneTimeTask<Unit> =
            Tasks.oneTime(ARKISTOINTI_JOB_TASK)
                .onFailure(handler)
                .onDeadExecution { completed, operations ->
                    versions.add(completed.execution.version)
                    if (recovered.getAndIncrement() == 0) {
                        // Both schedulers must read the same dead execution before either reschedules it.
                        check(secondDetected.await(10, TimeUnit.SECONDS))
                        handler.onFailure(completed, operations)
                    } else {
                        secondDetected.countDown()
                        try {
                            check(releaseStale.await(10, TimeUnit.SECONDS))
                            handler.onFailure(completed, operations)
                        } catch (exception: Throwable) {
                            staleError.set(exception)
                        } finally {
                            staleFinished.countDown()
                        }
                    }
                }
                .execute { instance, context -> delegate.executeOnce(instance, context) }
    }

    private fun assertTerminal(jobId: Long, errorCode: String) {
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
        assertThat(jobs.findById(jobId).orElseThrow().errorCode).isEqualTo(errorCode)
    }

    private fun simulateCrash(jobId: Long, completed: Boolean = false) {
        transactions.executeWithoutResult {
            if (!completed) jobs.findById(jobId).orElseThrow().transitionTo(ArkistointiJobStatus.KASITTELYSSA)
            jdbc.update("update scheduled_tasks set picked = true, picked_by = 'dead-process', last_heartbeat = ? " +
                "where task_instance = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(60)), jobId.toString())
        }
    }

    private fun scheduler(knownTask: OneTimeTask<Unit> = task, heartbeat: Duration = Duration.ofSeconds(1)): Scheduler {
        val autoCommitDataSource = object : DelegatingDataSource(dataSource) {
            override fun getConnection(): Connection = super.getConnection().apply { autoCommit = true }
        }
        return Scheduler.create(autoCommitDataSource, knownTask)
            .schedulerName(SchedulerName.Fixed("archive-it-${UUID.randomUUID()}"))
            .threads(1).pollingInterval(Duration.ofHours(1)).heartbeatInterval(heartbeat)
            .missedHeartbeatsLimit(4).shutdownMaxWait(Duration.ofSeconds(3)).build()
            .also { schedulers.add(it); it.start() }
    }

    private fun runUntil(scheduler: Scheduler, assertion: () -> Unit) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100)).untilAsserted {
            scheduler.triggerCheckForDueExecutions()
            assertion()
        }
    }

    private fun status(jobId: Long) = jobs.findById(jobId).orElseThrow().status
    private fun taskCount(jobId: Long) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_instance = ?", Long::class.javaObjectType, jobId.toString()
    )!!
    private fun failures(jobId: Long) = jdbc.queryForObject(
        "select coalesce(consecutive_failures, 0) from scheduled_tasks where task_instance = ?", Int::class.javaObjectType, jobId.toString()
    )!!

    private fun fixture(university: YliopistoEnum = YliopistoEnum.TURUN_YLIOPISTO): Long = transactions.execute {
        YliopistoHelper.createReferenceData(em, university)
        val source = AsiakirjaHelper.createEntity(em)
        val bytes = ByteArrayOutputStream()
        pdfA.createDocument(bytes).use { document -> document.addNewPage() }
        source.asiakirjaData!!.data = bytes.toByteArray()
        em.persist(source)
        val reference = snapshots.createReference(source, RecordType.YHTEENVETO, 0)
        creator.create(CreateArkistointiJobRequest(
            university, CaseType.VALMISTUMINEN, "{\"approved\":true}", "processing-${UUID.randomUUID()}", listOf(reference)
        )).id!!
    }

    private fun retryable() = ArkistointiResult.RetryableFailure(ArkistointiError.create("UNAVAILABLE", "Palvelu ei vastaa"))

    class TestAdapter : ArkistointiAdapter {
        override val university = YliopistoEnum.TURUN_YLIOPISTO
        val submissions = CopyOnWriteArrayList<ArkistointiSubmission>()
        @Volatile var behavior: (ArkistointiSubmission) -> ArkistointiResult = { ArkistointiResult.Success(null) }
        override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
            check(!TransactionSynchronizationManager.isActualTransactionActive()) { "Network call inside transaction" }
            submissions.add(pyynto)
            return behavior(pyynto)
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean fun testAdapter() = TestAdapter()
        @Bean fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:archive-processing-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }
}

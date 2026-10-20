package fi.elsapalvelu.elsa.service.arkistointi.job

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerName
import com.github.kagkarlsson.scheduler.exceptions.ExecutionException
import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.support.TransactionTemplate
import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.sql.DataSource

/** Fault injection and SQL writes are confined to the disposable PostgreSQL test database. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///archiveProcessing?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "db-scheduler.enabled=false",
    "application.arkistointi.retry.initial-delay=1s",
    "application.arkistointi.retry.max-delay=2s",
    "application.arkistointi.retry.max-attempts=3"
])
@Import(ArkistointiJobProcessingIT.TestConfig::class)
class ArkistointiSchedulerRecoveryIT {
    @Autowired private lateinit var creator: ArkistointiJobCreator
    @Autowired private lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var pdfA: PdfA2bService
    @Autowired private lateinit var adapter: ArkistointiJobProcessingIT.TestAdapter
    @Autowired private lateinit var task: OneTimeTask<Unit>
    @Autowired private lateinit var failureHandler: ArkistointiJobFailureHandler
    @Autowired private lateinit var dataSource: DataSource
    @MockitoSpyBean private lateinit var loader: ArkistointiAsiakirjaLoader
    private val schedulers = mutableListOf<Scheduler>()

    @BeforeEach
    fun resetAdapter() {
        adapter.submissions.clear()
        adapter.behavior = { ArkistointiResult.Success("external-id") }
    }

    @AfterEach
    fun cleanup() {
        schedulers.forEach(Scheduler::stop)
        transactions.executeWithoutResult {
            jdbc.execute("DROP TRIGGER IF EXISTS fail_archive_remove ON scheduled_tasks")
            jdbc.execute("DROP FUNCTION IF EXISTS fail_archive_remove()")
            jdbc.update("delete from scheduled_tasks where task_name = ?", ARKISTOINTI_JOB_TASK.taskName)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `failed scheduler removal recovers without changing a committed terminal result`(permanent: Boolean) {
        val jobId = fixture()
        if (permanent) {
            adapter.behavior = {
                ArkistointiResult.PermanentFailure(ArkistointiError.create("REJECTED", "Aineisto hylattiin"))
            }
        }
        blockRemoval()
        val first = scheduler()
        val terminal = if (permanent) ArkistointiJobStatus.VAATII_TOIMENPITEITA else ArkistointiJobStatus.LAHETETTY
        runUntil(first) {
            assertThat(jobs.findById(jobId).orElseThrow().status).isEqualTo(terminal)
            assertThat(schedulerState(jobId)["picked"]).isEqualTo(true)
            assertThat(first.currentlyExecuting).isEmpty()
        }
        val committed = jobState(jobId)
        val stored = jobs.findById(jobId).orElseThrow()
        assertThat(stored.externalId).isEqualTo(if (permanent) null else "external-id")
        assertThat(stored.errorCode).isEqualTo(if (permanent) "REJECTED" else null)
        if (permanent) assertThat(stored.completedAt).isNull() else assertThat(stored.completedAt).isNotNull()
        assertThat(taskCount(jobId)).isEqualTo(1)
        assertThat(adapter.submissions).hasSize(1)
        first.stop()
        transactions.executeWithoutResult {
            jdbc.execute("DROP TRIGGER fail_archive_remove ON scheduled_tasks")
            jdbc.execute("DROP FUNCTION fail_archive_remove()")
        }
        markDead(jobId)
        val recovery = scheduler()
        runUntil(recovery) { assertThat(taskCount(jobId)).isZero() }
        assertThat(jobState(jobId)).isEqualTo(committed)
        assertThat(adapter.submissions).hasSize(1)
        verify(loader, times(1)).load(any())
    }

    @Test
    fun `two recovery handlers can commit the same version but only one reschedules it`() {
        val jobId = fixture()
        markDead(jobId)
        val original = schedulerState(jobId)
        val gate = RescheduleGate()
        val recoveryTask = gate.task(task, failureHandler)
        val first = scheduler(recoveryTask)
        scheduler(recoveryTask)
        try {
            assertThat(gate.bothPrepared.await(10, TimeUnit.SECONDS)).isTrue()
            assertThat(gate.versions).hasSize(2).containsOnly((original["version"] as Number).toLong())
            assertThat(schedulerState(jobId)).isEqualTo(original)
            assertThat(jobs.findById(jobId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            gate.releaseSecond.countDown()
            assertThat(gate.secondFinished.await(10, TimeUnit.SECONDS)).isTrue()
            assertThat(gate.secondError.get()).isNull()
            val rescheduled = schedulerState(jobId)
            assertThat(rescheduled["picked"]).isEqualTo(false)
            assertThat((rescheduled["version"] as Number).toLong()).isEqualTo((original["version"] as Number).toLong() + 1)
            assertThat((rescheduled["consecutive_failures"] as Number).toInt()).isEqualTo(1)
            val committed = jobState(jobId)
            gate.releaseFirst.countDown()
            assertThat(gate.firstFinished.await(10, TimeUnit.SECONDS)).isTrue()
            assertThat(gate.firstError.get()).isInstanceOf(ExecutionException::class.java)
            assertThat(schedulerState(jobId)).isEqualTo(rescheduled)
            assertThat(jobState(jobId)).isEqualTo(committed)
        } finally {
            gate.releaseFirst.countDown()
            gate.releaseSecond.countDown()
        }
        runUntil(first) { assertThat(taskCount(jobId)).isZero() }
        assertThat(jobs.findById(jobId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(1)
        verify(loader, times(1)).load(any())
    }

    @Test
    fun `independent operations with identical payloads deliver distinct persisted UUIDs`() {
        val firstId = fixture()
        val secondId = fixture()
        val scheduler = scheduler()

        runUntil(scheduler) {
            assertThat(taskCount(firstId)).isZero()
            assertThat(taskCount(secondId)).isZero()
        }

        assertThat(adapter.submissions.map { it.key }).containsExactlyInAnyOrder(firstId, secondId)
        assertThat(adapter.submissions.map { it.jobId }).containsExactlyInAnyOrder(firstId, secondId)
        assertThat(adapter.submissions.map { it.payload }.distinct()).hasSize(1)
        assertThat(jobs.findById(firstId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(jobs.findById(secondId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
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
        assertThat(jobs.findById(jobId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(1)
        assertThat(adapter.submissions.single().key).isEqualTo(jobId)
    }

    @ParameterizedTest
    @CsvSource("DOCUMENT,true", "DATABASE,true", "DOCUMENT,false", "DATABASE,false")
    fun `document processing and database failures retry safely and respect the scheduler limit`(kind: String, recover: Boolean) {
        val jobId = fixture()
        val attempts = AtomicInteger()
        val secret = "private-background-filename-and-document-contents"
        doAnswer { invocation ->
            val attempt = attempts.incrementAndGet()
            if (recover && attempt > 1) invocation.callRealMethod() else {
                if (kind == "DATABASE") throw DataAccessResourceFailureException(secret, IllegalStateException(secret))
                throw ArkistointiAsiakirjaException(ArkistointiError.create("ARCHIVE_DOCUMENT_PROCESSING_FAILED", secret),
                    ArkistointiAsiakirjaErrorCategory.PROCESSING_FAILURE).also { it.addSuppressed(IllegalStateException(secret)) }
            }
        }.`when`(loader).load(any())
        ArkistointiSafeLogCapture().use { logs ->
            val worker = scheduler()
            runUntil(worker) {
                assertThat(jdbc.queryForObject("select consecutive_failures from scheduled_tasks where task_instance = ?",
                    Int::class.javaObjectType, jobId.toString())).isEqualTo(1)
            }
            val waiting = jobs.findById(jobId).orElseThrow()
            assertThat(waiting.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            assertThat(waiting.lastError).doesNotContain(secret)
            assertThat(adapter.submissions).isEmpty()
            runUntil(worker) { assertThat(taskCount(jobId)).isZero() }
            val result = jobs.findById(jobId).orElseThrow()
            if (recover) {
                assertThat(result.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
                assertThat(result.errorCode).isNull()
                assertThat(result.lastError).isNull()
                assertThat(adapter.submissions.map { it.key }).containsExactly(jobId)
                assertThat(attempts.get()).isEqualTo(2)
            } else {
                assertThat(result.status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
                assertThat(result.errorCode).isEqualTo("ARCHIVE_RETRIES_EXHAUSTED")
                assertThat(result.lastError).doesNotContain(secret)
                assertThat(adapter.submissions).isEmpty()
                assertThat(attempts.get()).isEqualTo(3)
            }
            logs.assertDoesNotContain(secret)
        }
    }

    private fun blockRemoval() = transactions.executeWithoutResult {
        jdbc.execute("""
            CREATE FUNCTION fail_archive_remove() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
            BEGIN RAISE EXCEPTION 'Test scheduler removal failure'; END; ${'$'}${'$'}
        """.trimIndent())
        jdbc.execute("CREATE TRIGGER fail_archive_remove BEFORE DELETE ON scheduled_tasks " +
            "FOR EACH ROW EXECUTE FUNCTION fail_archive_remove()")
    }

    private fun markDead(jobId: UUID) = transactions.executeWithoutResult {
        jdbc.update("update scheduled_tasks set picked = true, picked_by = 'dead-process', last_heartbeat = ? " +
            "where task_instance = ?", Timestamp.from(Instant.now().minusSeconds(60)), jobId.toString())
    }

    private fun fixture(): UUID = transactions.execute {
        YliopistoHelper.createReferenceData(em, YliopistoEnum.TURUN_YLIOPISTO)
        val source = AsiakirjaHelper.createEntity(em)
        val bytes = ByteArrayOutputStream()
        pdfA.createDocument(bytes).use { document -> document.addNewPage() }
        source.asiakirjaData!!.data = bytes.toByteArray()
        em.persist(source)
        val reference = snapshots.createReference(source, RecordType.YHTEENVETO, 0)
        creator.create(CreateArkistointiJobRequest(
            YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{\"approved\":true}",
            "recovery-${UUID.randomUUID()}", listOf(reference)
        )).id!!
    }

    private fun scheduler(knownTask: OneTimeTask<Unit> = task): Scheduler {
        val autoCommitDataSource = object : DelegatingDataSource(dataSource) {
            override fun getConnection(): Connection = super.getConnection().apply { autoCommit = true }
        }
        return Scheduler.create(autoCommitDataSource, knownTask)
            .schedulerName(SchedulerName.Fixed("archive-recovery-it-${UUID.randomUUID()}"))
            .threads(1).pollingInterval(Duration.ofHours(1)).heartbeatInterval(Duration.ofSeconds(1))
            .missedHeartbeatsLimit(4).shutdownMaxWait(Duration.ofSeconds(3)).build()
            .also { schedulers.add(it); it.start() }
    }

    private fun runUntil(scheduler: Scheduler, assertion: () -> Unit) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100)).untilAsserted {
            scheduler.triggerCheckForDueExecutions()
            assertion()
        }
    }

    private fun taskCount(jobId: UUID) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_instance = ?", Long::class.javaObjectType, jobId.toString()
    )!!

    private fun schedulerState(jobId: UUID) = jdbc.queryForMap(
        "select version, picked, execution_time, consecutive_failures from scheduled_tasks where task_instance = ?",
        jobId.toString()
    )

    private fun jobState(jobId: UUID) = jobs.findById(jobId).orElseThrow().let {
        listOf(it.status, it.externalId, it.completedAt, it.errorCode, it.lastError)
    }

    private class RescheduleGate {
        val bothPrepared = CountDownLatch(2)
        val releaseFirst = CountDownLatch(1)
        val releaseSecond = CountDownLatch(1)
        val firstFinished = CountDownLatch(1)
        val secondFinished = CountDownLatch(1)
        val firstError = AtomicReference<Throwable>()
        val secondError = AtomicReference<Throwable>()
        val versions = CopyOnWriteArrayList<Long>()
        private val firstPrepared = CountDownLatch(1)
        private val invocations = AtomicInteger()

        fun task(delegate: OneTimeTask<Unit>, handler: ArkistointiJobFailureHandler): OneTimeTask<Unit> =
            Tasks.oneTime(ARKISTOINTI_JOB_TASK).onFailure(handler)
                .onDeadExecution { completed, operations ->
                    val index = invocations.getAndIncrement()
                    check(index < 2) { "Unexpected extra recovery before the gated handlers finished" }
                    versions.add(completed.execution.version)
                    try {
                        if (index == 1) check(firstPrepared.await(10, TimeUnit.SECONDS))
                        handler.onFailure(completed, gatedOperations(operations, index))
                    } catch (exception: Throwable) {
                        (if (index == 0) firstError else secondError).set(exception)
                    } finally {
                        (if (index == 0) firstFinished else secondFinished).countDown()
                    }
                }
                .execute { instance, context -> delegate.executeOnce(instance, context) }

        private fun gatedOperations(operations: ExecutionOperations<Unit>, index: Int): ExecutionOperations<Unit> {
            // Spy only inserts a latch at the API boundary. callRealMethod uses the actual scheduler repository.
            val gated = spy(operations)
            doAnswer { invocation ->
                if (index == 0) firstPrepared.countDown()
                bothPrepared.countDown()
                check((if (index == 0) releaseFirst else releaseSecond).await(10, TimeUnit.SECONDS))
                invocation.callRealMethod()
            }.whenever(gated).reschedule(any<ExecutionComplete>(), any<Instant>())
            return gated
        }
    }
}

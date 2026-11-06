package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import fi.elsapalvelu.elsa.service.arkistointi.support.ArchiveProcessingTestSupport
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerName
import com.github.kagkarlsson.scheduler.task.CompletionHandler
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaErrors
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaException
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaLoader
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import jakarta.persistence.EntityManager
import java.sql.Connection
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.support.TransactionTemplate

/** All SQL writes simulate recovery in the isolated disposable PostgreSQL test database. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///archiveProcessing?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "db-scheduler.enabled=false",
    "application.arkistointi.retry.initial-delay=1s",
    "application.arkistointi.retry.max-delay=2s",
    "application.arkistointi.retry.max-attempts=3"
])
@Import(ArkistointiJobProcessingItBase.TestConfig::class)
class ArkistointiLoadingRecoveryIT : ArchiveProcessingTestSupport() {
    @Autowired private lateinit var creator: ArkistointiJobCreator
    @Autowired private lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var task: OneTimeTask<Unit>
    @Autowired private lateinit var dataSource: DataSource
    @Autowired private lateinit var state: ArkistointiJobStateService
    @MockitoSpyBean private lateinit var loader: ArkistointiAsiakirjaLoader
    private val schedulers = mutableListOf<Scheduler>()

    @BeforeEach
    fun resetAdapter() {
        adapter.submissions.clear()
        adapter.behavior = { ArkistointiResult.Success("new-external-id") }
    }

    @AfterEach
    fun stopSchedulers() {
        schedulers.forEach(Scheduler::stop)
        transactions.executeWithoutResult {
            jdbc.update("delete from scheduled_tasks where task_name = ?", ARKISTOINTI_JOB_TASK.taskName)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `heartbeat recovery parks paused loading and old attempt cannot send or overwrite recovery`(loadingFails: Boolean) {
        val loadingEntered = CountDownLatch(1)
        val releaseLoading = CountDownLatch(1)
        val loads = AtomicInteger()
        val versions = CopyOnWriteArrayList<Long>()
        doAnswer { invocation ->
            if (loads.getAndIncrement() == 0) {
                loadingEntered.countDown()
                check(releaseLoading.await(30, TimeUnit.SECONDS))
                if (loadingFails) throw ArkistointiAsiakirjaException(ArkistointiAsiakirjaErrors.checksumMismatch())
            }
            invocation.callRealMethod()
        }.`when`(loader).load(any())
        val jobId = fixture()
        val first = scheduler(observedTask(versions), Duration.ofHours(1))
        try {
            assertThat(loadingEntered.await(10, TimeUnit.SECONDS)).isTrue()
            // The previous implementation cannot acquire these locks until loading is released.
            // NOWAIT makes that regression fail deterministically rather than hanging the test.
            transactions.executeWithoutResult {
                jdbc.queryForMap("select version from scheduled_tasks where task_instance = ? for update nowait", jobId.toString())
                jdbc.queryForMap("select id from arkistointi_job where id = ? for update nowait", jobId)
                assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.KASITTELYSSA)
                jdbc.update("update scheduled_tasks set last_heartbeat = clock_timestamp() - interval '60 seconds' " +
                    "where task_instance = ?", jobId.toString())
            }
            val second = scheduler(observedTask(versions))
            runUntil(second) {
                val job = jobs.findById(jobId).orElseThrow()
                assertThat(job.status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
                assertThat(job.errorCode).isEqualTo("ARCHIVE_EXECUTION_LOST")
                val schedule = schedulerStateOrEmpty(jobId)
                assertThat(schedule["picked"]).isEqualTo(false)
                assertThat((schedule["execution_time"] as java.sql.Timestamp).toInstant()).isEqualTo(Schedule.NEVER)
            }
            assertThat(versions).hasSize(1)
            assertThat(loads.get()).isEqualTo(1)
            assertThat(adapter.submissions).isEmpty()
            val beforeSchedule = schedulerStateOrEmpty(jobId)
            val beforeJob = jobState(jobId)
            releaseLoading.countDown()
            await().atMost(Duration.ofSeconds(10)).untilAsserted { assertThat(first.currentlyExecuting).isEmpty() }
            assertThat(adapter.submissions).isEmpty()
            assertThat(schedulerStateOrEmpty(jobId)).isEqualTo(beforeSchedule)
            assertThat(jobState(jobId)).isEqualTo(beforeJob)
            assertThat(versions).hasSize(1)
            assertThat(loads.get()).isEqualTo(1)
        } finally {
            releaseLoading.countDown()
            schedulers.forEach(Scheduler::stop)
        }
    }

    @Test
    fun `attempt does not acknowledge recovery pending reschedule with the same version`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        doAnswer { invocation ->
            entered.countDown()
            check(release.await(20, TimeUnit.SECONDS))
            invocation.callRealMethod()
        }.`when`(loader).load(any())
        val jobId = fixture()
        val versions = CopyOnWriteArrayList<Long>()
        val first = scheduler(observedTask(versions), Duration.ofHours(1))
        try {
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue()
            val execution = ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, jobId.toString(), versions.single())
            assertThat(state.fail(execution, ArkistointiError.create("RECOVERING", "Uudelleenyritys"), stopRetrying = false)).isTrue()
            val before = schedulerStateOrEmpty(jobId)
            release.countDown()
            await().atMost(Duration.ofSeconds(10)).untilAsserted { assertThat(first.currentlyExecuting).isEmpty() }
            assertThat(adapter.submissions).isEmpty()
            assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            assertThat(schedulerStateOrEmpty(jobId)).isEqualTo(before)
        } finally {
            release.countDown()
            first.stop()
        }
    }

    private fun schedulerStateOrEmpty(jobId: UUID): Map<String, Any?> =
        if (taskCount(jobId) == 0L) emptyMap() else jdbc.queryForMap(
            "select version, picked, execution_time, consecutive_failures from scheduled_tasks where task_instance = ?",
            jobId.toString()
        )

    private fun jobState(jobId: UUID) = jobs.findById(jobId).orElseThrow().let {
        listOf(it.status, it.externalId, it.completedAt, it.errorCode, it.lastError)
    }

    private fun status(jobId: UUID) = jobs.findById(jobId).orElseThrow().status

    private fun taskCount(jobId: UUID) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_instance = ?", Long::class.javaObjectType, jobId.toString()
    )!!

    private fun fixture(): UUID = transactions.execute {
        YliopistoHelper.createReferenceData(em, YliopistoEnum.TURUN_YLIOPISTO)
        val source = AsiakirjaHelper.createEntity(em)
        source.asiakirjaData!!.data = PdfTestData.certificate()
        em.persist(source)
        val reference = snapshots.createReference(source, RecordType.YHTEENVETO, 0)
        creator.create(CreateArkistointiJobRequest(
            YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{\"approved\":true}",
            "loading-${UUID.randomUUID()}", listOf(reference)
        )).id!!
    }

    private fun observedTask(versions: MutableList<Long>): OneTimeTask<Unit> =
        object : OneTimeTask<Unit>(ARKISTOINTI_JOB_TASK.taskName, Unit::class.java, task.failureHandler, task.deadExecutionHandler) {
            override fun executeOnce(instance: TaskInstance<Unit>, context: ExecutionContext) =
                task.executeOnce(instance, context)

            override fun execute(instance: TaskInstance<Unit>, context: ExecutionContext): CompletionHandler<Unit> {
                versions.add(context.execution.version)
                return task.execute(instance, context)
            }
        }

    private fun scheduler(knownTask: OneTimeTask<Unit>, heartbeat: Duration = Duration.ofSeconds(1)): Scheduler {
        val autoCommitDataSource = object : DelegatingDataSource(dataSource) {
            override fun getConnection(): Connection = super.getConnection().apply { autoCommit = true }
        }
        return Scheduler.create(autoCommitDataSource, knownTask)
            .schedulerName(SchedulerName.Fixed("loading-it-${UUID.randomUUID()}"))
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
}

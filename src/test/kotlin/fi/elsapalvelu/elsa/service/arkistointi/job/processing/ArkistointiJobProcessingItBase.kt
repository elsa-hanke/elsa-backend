package fi.elsapalvelu.elsa.service.arkistointi.job.processing

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerName
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.job.ArkistointiError
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.CreateArkistointiJobRequest
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaLoader
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.support.ArchiveProcessingTestSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import jakarta.persistence.EntityManager
import java.net.URI
import java.sql.Connection
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.cache.Caching
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.support.TransactionTemplate

/**
 * Shared fixture for archive job processing ITs: real db-scheduler schedulers, simulated crashes and
 * SQL inspection of `scheduled_tasks`. All SQL writes target this isolated disposable test database.
 * Subclasses share one Spring context because the annotations below are inherited.
 */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///archiveProcessing?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "db-scheduler.enabled=false",
    "application.arkistointi.retry.initial-delay=1s",
    "application.arkistointi.retry.max-delay=2s",
    "application.arkistointi.retry.max-attempts=3"
])
@Import(ArkistointiJobProcessingItBase.TestConfig::class)
open class ArkistointiJobProcessingItBase : ArchiveProcessingTestSupport() {
    @Autowired protected lateinit var creator: ArkistointiJobCreator
    @Autowired protected lateinit var jobScheduler: ArkistointiJobScheduler
    @Autowired protected lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @Autowired protected lateinit var jobs: ArkistointiJobRepository
    @Autowired protected lateinit var em: EntityManager
    @Autowired protected lateinit var transactions: TransactionTemplate
    @Autowired protected lateinit var jdbc: JdbcTemplate
    @Autowired protected lateinit var task: OneTimeTask<Unit>
    @Autowired protected lateinit var dataSource: DataSource
    @Autowired protected lateinit var processor: ArkistointiJobProcessor
    @Autowired protected lateinit var failureHandler: ArkistointiJobFailureHandler
    @Autowired protected lateinit var state: ArkistointiJobStateService
    @MockitoSpyBean protected lateinit var loader: ArkistointiAsiakirjaLoader
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

    // --- Job fixtures ---

    protected fun fixture(
        university: YliopistoEnum = YliopistoEnum.TURUN_YLIOPISTO,
        payload: String = "{\"approved\":true}",
        checksum: String? = null,
        caseType: CaseType = CaseType.VALMISTUMINEN
    ): UUID = transactions.execute {
        YliopistoHelper.createReferenceData(em, university)
        val source = AsiakirjaHelper.createEntity(em)
        source.asiakirjaData!!.data = PdfTestData.certificate()
        em.persist(source)
        val reference = snapshots.createReference(source, RecordType.YHTEENVETO, 0)
        val request = CreateArkistointiJobRequest(
            university, caseType, payload, "processing-${UUID.randomUUID()}", listOf(reference)
        )
        if (checksum == null) {
            creator.create(request).id!!
        } else {
            // Seed a malformed historical manifest at INSERT time. Never disable immutability
            // triggers or mutate a committed delivery merely to exercise corruption recovery.
            val job = ArkistointiJob(university = university, caseType = request.caseType, payload = payload,
                key = request.key, expectedDocumentCount = 1)
            job.lisaaAsiakirja(ArkistointiJobAsiakirja(asiakirja = reference.asiakirja,
                asiakirjatyyppi = reference.asiakirjatyyppi, jarjestysnumero = reference.jarjestysnumero,
                filename = reference.filename, contentType = reference.contentType, sha256 = checksum))
            val id = requireNotNull(jobs.saveAndFlush(job).id)
            check(jobScheduler.schedule(id))
            id
        }
    }

    protected fun retryable() = ArkistointiResult.RetryableFailure(ArkistointiError.create("UNAVAILABLE", "Palvelu ei vastaa"))

    /** An adapter outcome that arrives after its execution may already have been replaced. */
    protected fun lateResult(result: String): ArkistointiResult = when (result) {
        "PERMANENT" -> ArkistointiResult.PermanentFailure(ArkistointiError.create("OLD_ERROR", "Vanha virhe"))
        "SUCCESS" -> ArkistointiResult.Success("old-external-id")
        "INVALID_SUCCESS" -> ArkistointiResult.Success("x".repeat(256))
        "RETRYABLE" -> retryable()
        else -> error("secret old response")
    }

    // --- Schedulers ---

    protected fun scheduler(knownTask: OneTimeTask<Unit> = task, heartbeat: Duration = Duration.ofSeconds(1)): Scheduler {
        val autoCommitDataSource = object : DelegatingDataSource(dataSource) {
            override fun getConnection(): Connection = super.getConnection().apply { autoCommit = true }
        }
        return Scheduler.create(autoCommitDataSource, knownTask)
            .schedulerName(SchedulerName.Fixed("archive-it-${UUID.randomUUID()}"))
            .threads(1).pollingInterval(Duration.ofHours(1)).heartbeatInterval(heartbeat)
            .missedHeartbeatsLimit(4).shutdownMaxWait(Duration.ofSeconds(3)).build()
            .also { schedulers.add(it); it.start() }
    }

    protected fun runUntil(scheduler: Scheduler, assertion: () -> Unit) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100)).untilAsserted {
            scheduler.triggerCheckForDueExecutions()
            assertion()
        }
    }

    // --- Scheduler table (scheduled_tasks) access ---

    protected fun execution(jobId: UUID) = ArkistointiJobExecution(
        ARKISTOINTI_JOB_TASK.taskName, jobId.toString(),
        jdbc.queryForObject("select version from scheduled_tasks where task_instance = ?", Long::class.javaObjectType, jobId.toString())!!
    )

    protected fun taskCount(jobId: UUID) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_instance = ?", Long::class.javaObjectType, jobId.toString()
    )!!

    protected fun failures(jobId: UUID) = jdbc.queryForObject(
        "select coalesce(consecutive_failures, 0) from scheduled_tasks where task_instance = ?", Int::class.javaObjectType, jobId.toString()
    )!!

    protected fun schedulerState(jobId: UUID) = jdbc.queryForMap(
        "select version, picked, execution_time, consecutive_failures from scheduled_tasks where task_instance = ?",
        jobId.toString()
    )

    protected fun schedulerStateOrEmpty(jobId: UUID) = if (taskCount(jobId) == 0L) emptyMap() else schedulerState(jobId)

    protected fun nextExecutionTime(jobId: UUID): Instant = jdbc.queryForObject(
        "select execution_time from scheduled_tasks where task_instance = ?", Timestamp::class.java, jobId.toString()
    )!!.toInstant()

    protected fun isPicked(jobId: UUID) = jdbc.queryForObject(
        "select picked from scheduled_tasks where task_instance = ?", Boolean::class.javaObjectType, jobId.toString()
    )!!

    /** Updates the scheduler row, e.g. `updateSchedulerRow(jobId, "picked = true, version = version + 1")`. */
    protected fun updateSchedulerRow(jobId: UUID, assignments: String, vararg args: Any) {
        transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set $assignments where task_instance = ?", *args, jobId.toString())
        }
    }

    /** Makes the scheduler row look like it belongs to a crashed process. */
    protected fun simulateCrash(jobId: UUID, completed: Boolean = false, processingStarted: Boolean = true) {
        transactions.executeWithoutResult {
            if (!completed && processingStarted) jobs.findById(jobId).orElseThrow().transitionTo(ArkistointiJobStatus.KASITTELYSSA)
            jdbc.update("update scheduled_tasks set picked = true, picked_by = 'dead-process', last_heartbeat = ? " +
                "where task_instance = ?", Timestamp.from(Instant.now().minusSeconds(60)), jobId.toString())
        }
    }

    /** Fails statements matching [condition] (a SQL boolean over `NEW`/`OLD`); a deferred trigger fails the COMMIT itself. */
    protected fun installFailingTrigger(name: String, table: String, condition: String, deferred: Boolean = false) {
        transactions.executeWithoutResult {
            jdbc.execute("""
                CREATE FUNCTION $name() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                BEGIN
                    IF $condition THEN RAISE EXCEPTION 'Test failure from $name'; END IF;
                    RETURN NEW;
                END; ${'$'}${'$'}
            """.trimIndent())
            val definition = if (deferred) "CONSTRAINT TRIGGER $name AFTER UPDATE" else "TRIGGER $name BEFORE UPDATE"
            val timing = if (deferred) " DEFERRABLE INITIALLY DEFERRED" else ""
            jdbc.execute("CREATE $definition ON $table$timing FOR EACH ROW EXECUTE FUNCTION $name()")
        }
    }

    protected fun dropFailingTrigger(name: String, table: String) {
        transactions.executeWithoutResult {
            jdbc.execute("DROP TRIGGER IF EXISTS $name ON $table")
            jdbc.execute("DROP FUNCTION IF EXISTS $name()")
        }
    }

    // --- Job state and assertions ---

    protected fun status(jobId: UUID) = jobs.findById(jobId).orElseThrow().status

    protected fun storedJob(jobId: UUID) = jobs.findById(jobId).orElseThrow()

    protected fun jobState(jobId: UUID) = storedJob(jobId).let {
        listOf(it.status, it.externalId, it.completedAt, it.errorCode, it.lastError)
    }

    protected fun assertSucceeded(jobId: UUID, externalId: String?) {
        val stored = storedJob(jobId)
        assertThat(stored.status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(stored.externalId).isEqualTo(externalId)
        assertThat(stored.completedAt).isNotNull()
        assertThat(stored.errorCode).isNull()
        assertThat(stored.lastError).isNull()
    }

    protected fun assertParked(jobId: UUID) {
        assertThat(status(jobId)).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
        val row = schedulerState(jobId)
        assertThat(row["picked"]).isEqualTo(false)
        assertThat((row["execution_time"] as Timestamp).toInstant()).isEqualTo(Schedule.NEVER)
    }

    protected fun assertTerminal(jobId: UUID, errorCode: String) {
        val scheduler = scheduler()
        runUntil(scheduler) { assertParked(jobId) }
        assertThat(storedJob(jobId).errorCode).isEqualTo(errorCode)
    }

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:archive-processing-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }
}

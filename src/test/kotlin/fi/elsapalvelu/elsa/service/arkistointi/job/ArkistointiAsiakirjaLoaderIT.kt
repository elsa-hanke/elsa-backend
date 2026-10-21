package fi.elsapalvelu.elsa.service.arkistointi.job

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerName
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.arkistointi.ARKISTOINTI_JOB_TASK
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.hibernate.SessionFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Connection
import java.time.Duration
import java.util.UUID
import javax.sql.DataSource

/** Uses the same isolated PostgreSQL Testcontainer and adapter as the processing integration tests. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///archiveProcessing?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "db-scheduler.enabled=false",
    "application.arkistointi.retry.initial-delay=1s",
    "application.arkistointi.retry.max-delay=2s",
    "application.arkistointi.retry.max-attempts=3"
])
@Import(ArkistointiJobProcessingIT.TestConfig::class)
class ArkistointiAsiakirjaLoaderIT {
    @Autowired private lateinit var creator: ArkistointiJobCreator
    @Autowired private lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var adapter: ArkistointiJobProcessingIT.TestAdapter
    @Autowired private lateinit var task: OneTimeTask<Unit>
    @Autowired private lateinit var dataSource: DataSource
    @Autowired private lateinit var state: ArkistointiJobStateService
    @MockitoSpyBean private lateinit var loader: ArkistointiAsiakirjaLoader
    private val schedulers = mutableListOf<Scheduler>()

    @BeforeEach
    fun resetAdapter() {
        adapter.submissions.clear()
        adapter.behavior = { ArkistointiResult.Success(null) }
    }

    @AfterEach
    fun stopSchedulers() {
        schedulers.forEach(Scheduler::stop)
        transactions.executeWithoutResult {
            jdbc.update("delete from scheduled_tasks where task_name = ?", ARKISTOINTI_JOB_TASK.taskName)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["PDF_100", "CMYK_JPEG"])
    fun `adapter receives scalar metadata and retry reloads unchanged database bytes`(kind: String) {
        val jobId = fixture(kind)
        val snapshotId = transactions.execute {
            jobs.findById(jobId).orElseThrow().asiakirjat.single().asiakirja!!.id!!
        }
        val original = storedDocumentBytes(snapshotId)
        val expectedChecksum = ArkistointiChecksum.sha256(original)
        adapter.behavior = { submission ->
            val document = submission.asiakirjat.single()
            assertThat(document.asiakirjaId).isEqualTo(snapshotId)
            assertThat(document.asiakirjatyyppi).isEqualTo(RecordType.YHTEENVETO)
            assertThat(document.jarjestysnumero).isZero()
            assertThat(document.filename).isEqualTo(if (kind == "CMYK_JPEG") "certificate.jpg" else "attachment.pdf")
            assertThat(document.contentType).isEqualTo(if (kind == "CMYK_JPEG") "image/jpg" else "application/pdf")
            assertThat(document.sha256).isEqualTo(expectedChecksum)
            assertThat(ArkistointiChecksum.sha256(document.sisalto)).isEqualTo(expectedChecksum)
            if (adapter.submissions.size == 1) {
                document.sisalto.fill(0)
                ArkistointiResult.RetryableFailure(ArkistointiError.create("UNAVAILABLE", "Palvelu ei vastaa"))
            } else {
                ArkistointiResult.Success(null)
            }
        }
        val scheduler = scheduler()
        runUntil(scheduler) { assertThat(failures(jobId)).isEqualTo(1) }
        assertThat(storedDocumentBytes(snapshotId)).containsExactly(*original)
        runUntil(scheduler) { assertThat(taskCount(jobId)).isZero() }
        assertThat(jobs.findById(jobId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
        assertThat(adapter.submissions).hasSize(2)
        assertThat(adapter.submissions[0].asiakirjat.single().sisalto).containsOnly(0)
        assertThat(adapter.submissions[1].asiakirjat.single().sisalto).containsExactly(*original)
    }

    @Test
    fun `preparation commits only scalar references without loading document entities or bytes`() {
        val jobId = fixture()
        transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set picked = true where task_instance = ?", jobId.toString())
        }
        val execution = ArkistointiJobExecution(ARKISTOINTI_JOB_TASK.taskName, jobId.toString(),
            jdbc.queryForObject("select version from scheduled_tasks where task_instance = ?", Long::class.javaObjectType, jobId.toString())!!)
        val statistics = em.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val previouslyEnabled = statistics.isStatisticsEnabled
        try {
            statistics.isStatisticsEnabled = true
            statistics.clear()
            val prepared = requireNotNull(state.prepare(execution))
            verifyNoInteractions(loader)
            assertThat(statistics.getEntityStatistics("fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja").loadCount).isZero()
            assertThat(statistics.getEntityStatistics("fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData").loadCount).isZero()
            assertThat(jobs.findById(jobId).orElseThrow().status).isEqualTo(ArkistointiJobStatus.KASITTELYSSA)
            assertThat(prepared.payload).isEqualTo("{\"approved\":true}")
            assertThat(prepared.toString()).doesNotContain(prepared.payload)
            val descriptor = prepared.asiakirjat.single()
            assertThat(descriptor.toString()).doesNotContain(descriptor.filename)
            // The persistence context used by prepare is closed; loading must need no proxies.
            assertThat(loader.load(descriptor).sisalto).containsExactly(*storedDocumentBytes(descriptor.asiakirjaId))
        } finally {
            statistics.isStatisticsEnabled = previouslyEnabled
        }
    }

    @Test
    fun `loading archived bytes does not materialize document entities`() {
        val jobId = fixture()
        val statistics = em.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val previouslyEnabled = statistics.isStatisticsEnabled
        try {
            statistics.isStatisticsEnabled = true
            transactions.executeWithoutResult {
                val reference = jobs.findById(jobId).orElseThrow().asiakirjat.single()
                em.flush()
                em.clear()
                statistics.clear()
                val loaded = loader.load(reference.toDescriptor())
                assertThat(loaded.sisalto).isNotEmpty()
                assertThat(statistics.getEntityStatistics("fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja").loadCount)
                    .isZero()
                assertThat(statistics.getEntityStatistics("fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData").loadCount)
                    .isZero()
            }
        } finally {
            statistics.isStatisticsEnabled = previouslyEnabled
        }
    }

    private fun fixture(kind: String = "PDF_100"): UUID = transactions.execute {
        YliopistoHelper.createReferenceData(em, YliopistoEnum.TURUN_YLIOPISTO)
        val source = AsiakirjaHelper.createEntity(em)
        source.asiakirjaData!!.data = if (kind == "CMYK_JPEG") PdfTestData.cmykJpeg() else PdfTestData.unembeddedFontPdf(100)
        source.nimi = if (kind == "CMYK_JPEG") "certificate.jpg" else "attachment.pdf"
        source.tyyppi = if (kind == "CMYK_JPEG") "image/jpg" else "application/pdf"
        em.persist(source)
        val reference = snapshots.createReference(source, RecordType.YHTEENVETO, 0)
        creator.create(CreateArkistointiJobRequest(
            YliopistoEnum.TURUN_YLIOPISTO, CaseType.VALMISTUMINEN, "{\"approved\":true}",
            "loader-${UUID.randomUUID()}", listOf(reference)
        )).id!!
    }

    private fun storedDocumentBytes(asiakirjaId: Long) = jdbc.queryForObject(
        "select d.data from asiakirja a join asiakirja_data d on d.id = a.asiakirja_data_id where a.id = ?",
        ByteArray::class.java, asiakirjaId
    )!!

    private fun scheduler(): Scheduler {
        val autoCommitDataSource = object : DelegatingDataSource(dataSource) {
            override fun getConnection(): Connection = super.getConnection().apply { autoCommit = true }
        }
        return Scheduler.create(autoCommitDataSource, task)
            .schedulerName(SchedulerName.Fixed("archive-loader-it-${UUID.randomUUID()}"))
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

    private fun failures(jobId: UUID) = jdbc.queryForObject(
        "select coalesce(consecutive_failures, 0) from scheduled_tasks where task_instance = ?",
        Int::class.javaObjectType, jobId.toString()
    )!!
}

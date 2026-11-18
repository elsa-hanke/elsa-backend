package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessorImpl
import fi.elsapalvelu.elsa.service.arkistointi.support.ArchiveSchedulerTestControl
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import com.fasterxml.jackson.databind.JsonNode
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.cache.CacheManager
import javax.imageio.ImageIO
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status



/** Real approval, PostgreSQL snapshots, production scheduler, adapter, ledger and HTTP client. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
 "spring.datasource.url=jdbc:tc:postgresql:16.9:///dynastyDelivery?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
 "application.arkistointi.turku.kaytossa=true",
 "db-scheduler.heartbeat-interval=1h",
 "application.arkistointi.turku.metadata.cases.koejakso.async-enabled=true",
 "application.arkistointi.turku.metadata.cases.koejakso.documents.arviointi.retention-period=-1",
 "application.arkistointi.turku.metadata.cases.valmistuminen.async-enabled=true",
 "application.arkistointi.turku.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
 "application.arkistointi.turku.metadata.cases.valmistuminen.documents.liite.retention-period=20",
 "application.arkistointi.turku.metadata.cases.valmistuminen.documents.laillistamistodistus.retention-period=20",
 "application.arkistointi.turku.dynasty.client-id=test",
 "application.arkistointi.turku.dynasty.client-secret=test",
 "application.arkistointi.turku.dynasty.organization=0",
 "application.arkistointi.turku.dynasty.case-schema-id=case",
 "application.arkistointi.turku.dynasty.document-schema-id=document",
 "application.arkistointi.turku.dynasty.allow-loopback-http-for-tests=true",
 "application.arkistointi.turku.dynasty.tutkintoasetukset[test]=Test regulation",
 "application.arkistointi.turku.dynasty.koejakso-publicity-class-id=test-secret",
 "application.arkistointi.turku.dynasty.koejakso-secrecy-fields[reason]=test-law",
 "application.arkistointi.turku.dynasty.koejakso-secrecy-fields[duration]=100",
 "application.arkistointi.turku.dynasty.koejakso-secrecy-fields[start]=approval"
])
@Import(DynastyDeliveryIT.CacheConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DynastyDeliveryIT : AsyncArchiveItSupport() {

 @Autowired private lateinit var properties: fi.elsapalvelu.elsa.config.ApplicationProperties
 @Autowired private lateinit var client: DynastyClient
 @Autowired private lateinit var task: com.github.kagkarlsson.scheduler.task.helper.OneTimeTask<Unit>
 @Autowired private lateinit var dataSource: javax.sql.DataSource
 @MockitoSpyBean private lateinit var processor: ArkistointiJobProcessorImpl
 @MockitoBean lateinit var mail: MailService
    private val stepFixture get() = DynastyStepJdbcFixture(jdbc)
    private lateinit var schedulerControl: ArchiveSchedulerTestControl

    @BeforeEach
    fun resetReceiver() {
        ReflectionTestUtils.setField(client, "token", null)
        schedulerControl = ArchiveSchedulerTestControl(scheduler, processor)
        pollAndAwait()

        server.dispatcher = QueueDispatcher()
        while (server.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // Remove requests left by a failed previous assertion.
        }
    }

    @AfterEach
    fun clearPendingExecutions() {
        server.dispatcher = QueueDispatcher()
        server.enqueue(MockResponse().setResponseCode(400))
        schedulerControl.releasePendingAttempts()
        await().atMost(Duration.ofSeconds(30)).until { scheduler.currentlyExecuting.isEmpty() }
        jdbc.queryForList("select task_instance from scheduled_tasks where task_name = 'arkistointi-job'", String::class.java)
            .forEach { scheduler.cancel(TaskInstanceId.of("arkistointi-job", it)) }
    }


    private fun job(): fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob {
        val id = approveKoejakso(CaseType.KOEJAKSO)
        return jobs.findByKey("koejakso-arvio:TURUN_YLIOPISTO:$id")!!
    }
    private fun enqueue(vararg bodies: String) = bodies.forEach { server.enqueue(MockResponse().setBody(it)) }
    @Test fun `scheduler delivers through real adapter and commits every ledger step`() {
        val job = job()
        server.enqueueDynastySuccess(documentIds = listOf(1078))
        startAttempt()
        awaitDelivered(job.id!!)
        assertThat(jobs.findById(job.id!!).orElseThrow().externalId).isEqualTo("415")
        assertThat(stepFixture.steps(job.id!!).map { it.status })
            .containsExactlyInAnyOrder(DynastyStepStatus.VALMIS, DynastyStepStatus.VALMIS, DynastyStepStatus.VALMIS, DynastyStepStatus.VALMIS)
    }
    @Test fun `success commit failure resumes completed ledger without another HTTP request`() {
        val job = job()
        withSuccessCommitFailure {
            server.enqueueDynastySuccess(documentIds = listOf(1078))
            startAttempt()
            await().atMost(Duration.ofSeconds(30)).untilAsserted {
                assertThat(scheduler.currentlyExecuting).isEmpty()
                val stored = jobs.findById(job.id!!).orElseThrow()
                assertThat(stored.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
                assertThat(stored.errorCode).isEqualTo("ARCHIVE_SUCCESS_PERSISTENCE_FAILED")
            }
        }
        val requests = server.requestCount
        transactions.executeWithoutResult {
            jdbc.update("update scheduled_tasks set execution_time=clock_timestamp() " +
                "where task_name='arkistointi-job' and task_instance=? and picked=false", job.id.toString())
        }
        startAttempt()
        awaitDelivered(job.id!!)
        assertThat(server.requestCount).isEqualTo(requests)
    }

    @Test fun `manual retry skips completed case and task after definite document rejection`() {
        val job = job()
        enqueue(DYNASTY_TEST_TOKEN_RESPONSE, "415", "\"${UUID.randomUUID()}\"")
        server.enqueue(MockResponse().setResponseCode(403))
        startAttempt()
        awaitParked(job.id!!)
        assertThat(stepFixture.steps(job.id!!).filter { it.status == DynastyStepStatus.VALMIS }.map { it.key })
            .containsExactlyInAnyOrder("CASE","TASK")
        retryParkedJob(job.id!!)
        enqueue("1078", "")
        startAttempt()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val result = jobs.findById(job.id!!).orElseThrow()
            assertThat(result.status)
                .describedAs("error=%s steps=%s",result.errorCode,stepFixture.steps(job.id!!))
                .isEqualTo(ArkistointiJobStatus.LAHETETTY)
        }
        val requests = (1..6).map { server.takeRequest(5, TimeUnit.SECONDS)!! }
        assertThat(requests.count { it.path!!.startsWith("/api/v1/cases?") }).isEqualTo(1)
        assertThat(requests.count { it.path!!.contains("/tasks") }).isEqualTo(1)
    }
    @Test fun `lost case response parks job and retry never creates another case`() {
        val job = job()
        enqueue(DYNASTY_TEST_TOKEN_RESPONSE)
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST))
        startAttempt()
        awaitParked(job.id!!)
        val before = server.requestCount
        retryParkedJob(job.id!!)
        startAttempt()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val pending = jobs.findById(job.id!!).orElseThrow()
            assertThat(pending.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            assertThat(pending.errorCode).isEqualTo("DYNASTY_STEP_IN_PROGRESS")
            assertThat(jdbc.queryForObject("select picked from scheduled_tasks where task_instance=?", Boolean::class.javaObjectType, job.id.toString())).isFalse()
        }
        assertThat(server.requestCount).isEqualTo(before)
        assertThat(stepFixture.step(job.id!!, "CASE")?.status).isEqualTo(DynastyStepStatus.ALOITETTU)
    }
    @Test fun `lost heartbeat during HTTP send parks job and late case response is recorded without another step`() {
        val job = job()
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val requestsBefore = server.requestCount
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/token") return MockResponse().setBody(DYNASTY_TEST_TOKEN_RESPONSE)
                entered.countDown()
                check(release.await(30, TimeUnit.SECONDS))
                return MockResponse().setBody("415")
            }
        }
        val recoveryDataSource = object : org.springframework.jdbc.datasource.DelegatingDataSource(dataSource) {
            override fun getConnection(): java.sql.Connection = super.getConnection().apply { autoCommit = true }
        }
        val recovery = com.github.kagkarlsson.scheduler.Scheduler.create(recoveryDataSource, task)
            .schedulerName(com.github.kagkarlsson.scheduler.SchedulerName.Fixed("dynasty-recovery-it"))
            .threads(1).pollingInterval(Duration.ofHours(1)).heartbeatInterval(Duration.ofSeconds(1))
            .missedHeartbeatsLimit(4).shutdownMaxWait(Duration.ofSeconds(3)).build()
        try {
            startAttempt()
            assertThat(entered.await(20, TimeUnit.SECONDS)).isTrue()
            val version = jdbc.queryForObject("select version from scheduled_tasks where task_instance=?", Long::class.javaObjectType, job.id.toString())!!
            // A second real scheduler detects the missing heartbeat while the HTTP request remains blocked.
            recovery.start()
            await().atMost(Duration.ofSeconds(15)).untilAsserted {
                val stored = jobs.findById(job.id!!).orElseThrow()
                assertThat(stored.status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
                assertThat(stored.errorCode).isEqualTo("ARCHIVE_EXECUTION_LOST")
                assertThat(jdbc.queryForObject("select picked from scheduled_tasks where task_instance=?",
                    Boolean::class.javaObjectType, job.id.toString())).isFalse()
            }
            assertThat(stepFixture.step(job.id!!, "CASE")?.executionVersion)
                .isEqualTo(version)
            release.countDown()
            await().atMost(Duration.ofSeconds(20)).untilAsserted {
                val step = requireNotNull(stepFixture.step(job.id!!, "CASE"))
                assertThat(step.status).isEqualTo(DynastyStepStatus.VALMIS)
                assertThat(step.remoteId).isEqualTo("415")
                assertThat(scheduler.currentlyExecuting).isEmpty()
            }
            assertThat(server.requestCount).isEqualTo(requestsBefore + 2)
            assertThat(stepFixture.steps(job.id!!).count { it.key == "TASK" }).isZero()
        } finally {
            release.countDown()
            recovery.stop()
        }
    }
    @ParameterizedTest(name = "{0} certificate as {1}")
    @CsvSource("EL,pdf", "EHL,pdf", "YEK,pdf", "YEK,png")
    fun `approved valmistuminen reaches Dynasty as case task and three documents in order`(kind: String, format: String) {
        val image = format == "png"
        val fixture = createValmistumispyyntoFixture(kind, YliopistoEnum.TURUN_YLIOPISTO, true)
        transactions.executeWithoutResult { _ ->
            val opintooikeus = em.find(Valmistumispyynto::class.java, fixture.first).opintooikeus!!
            properties.getArkistointi().getTurku().dynasty.tutkintoasetukset = mapOf(opintooikeus.asetus!!.nimi!! to "Test regulation")
            opintooikeus.erikoistuvaLaakari!!.apply {
                laillistamistodistus = AsiakirjaData(data = if (image) certificateImage else certificate)
                laillistamispaivanLiitetiedostonNimi = "certificate.$format"
                laillistamispaivanLiitetiedostonTyyppi = if (image) "image/png" else "application/pdf"
            }
        }
        setSecurityContext(fixture.second, VASTUUHENKILO)
        approveValmistumispyynto(fixture.first).andExpect(status().isOk)
        val job = jobs.findByKey("valmistuminen:TURUN_YLIOPISTO:${fixture.first}")!!
        val taskId = UUID.randomUUID()
        server.enqueueDynastySuccess(documentIds = listOf(1078, 1079, 1080), taskId = taskId)
        val requestsBefore = server.requestCount

        startAttempt()
        awaitDelivered(job.id!!)

        assertThat(server.requestCount - requestsBefore).describedAs("token, case, task, three documents, archive and nothing else").isEqualTo(7)
        val requests = (1..7).map { server.takeRequest(5, TimeUnit.SECONDS)!! }
        assertThat(requests.map { it.method + " " + it.path!!.substringBefore('?') }).containsExactly(
            "POST /token", "POST /api/v1/cases", "POST /api/v1/cases/415/tasks",
            "POST /api/v1/documents", "POST /api/v1/documents", "POST /api/v1/documents",
            "POST /api/v1/cases/415/state/Archived"
        )
        val bodies = requests.map { it.body.readUtf8() }
        // The program comes from the real approval payload; the full metadata mapping is covered by the golden tests.
        assertThat(objectMapper.readTree(bodies[1]).at("/caseModel/title").asText()).startsWith("$kind ")
        val main = multipartJson(bodies[3], "CaseAttachmentOptions")
        assertThat(main.at("/CaseSelector/Id").asText()).isEqualTo("415")
        assertThat(main.at("/PhaseSelector/Id").asText()).isEqualTo(taskId.toString())
        listOf(bodies[4], bodies[5]).forEach { body ->
            val options = multipartJson(body, "CaseAttachmentOptions")
            assertThat(options.at("/caseSelector/id").asLong()).isEqualTo(415)
            assertThat(options.at("/parentDocumentSelector/id").asLong()).isEqualTo(1078)
        }
        assertThat(bodies[5]).contains("filename=\"certificate.$format\"", "Content-Type: ${if (image) "image/png" else "application/pdf"}")
        assertThat(jobs.findById(job.id!!).orElseThrow().externalId).isEqualTo("415")
        assertThat(stepFixture.steps(job.id!!).associate { it.key to it.status }).isEqualTo(
            listOf("CASE", "TASK", "DOCUMENT:0", "DOCUMENT:1", "DOCUMENT:2", "ARCHIVE_CASE").associateWith { DynastyStepStatus.VALMIS }
        )
        assertThat((0..2).map { stepFixture.step(job.id!!, "DOCUMENT:$it")?.remoteId }).containsExactly("1078", "1079", "1080")
    }

    private fun multipartJson(body: String, part: String): JsonNode = objectMapper.readTree(
        Regex("name=\"$part\"\\r\\n(?:[A-Za-z-]+: [^\\r\\n]*\\r\\n)*\\r\\n(.*?)\\r\\n--", RegexOption.DOT_MATCHES_ALL)
            .find(body)!!.groupValues[1]
    )

    private fun approveKoejakso(type: CaseType): Long {
        val fixture = koejaksoFixture(YliopistoEnum.TURUN_YLIOPISTO, type)
        transactions.executeWithoutResult {
            val opintooikeus = vastuuhenkilonArvioRepository.findById(fixture.second).orElseThrow().opintooikeus!!
            properties.getArkistointi().getTurku().dynasty.tutkintoasetukset = mapOf(opintooikeus.asetus!!.nimi!! to "Test regulation")
        }
        val result = if (type == CaseType.SOPIMUS) approveKoejaksonKoulutussopimus(fixture) else approveKoejaksonVastuuhenkilonArvio(fixture)
        result.andExpect(status().isOk)
        return if (type == CaseType.SOPIMUS) fixture.first else fixture.second
    }

    private fun startAttempt() = schedulerControl.startAttempt()

    private fun pollAndAwait() = schedulerControl.pollAndAwait()

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): CacheManager =
            AsyncArchiveItSupport.isolatedArchiveCache("urn:elsa:dynasty-delivery-it", javaClass.classLoader, customizer)
    }

    companion object {
        private val server = MockWebServer().apply { start() }
        private val certificate = PdfTestData.certificate()
        private val certificateImage = ByteArrayOutputStream().use { output ->
            check(ImageIO.write(BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", output))
            output.toByteArray()
        }
        @JvmStatic @DynamicPropertySource
        fun transport(registry: DynamicPropertyRegistry) {
            registry.add("application.arkistointi.turku.dynasty.base-url") { server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/') }
            registry.add("application.arkistointi.turku.dynasty.token-url") { server.url("/token").newBuilder().host("127.0.0.1").build().toString() }
        }
        @JvmStatic @AfterAll fun stopServer() = server.shutdown()
    }
}

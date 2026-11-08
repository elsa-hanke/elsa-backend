package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.processing.ArkistointiJobProcessorImpl
import fi.elsapalvelu.elsa.service.arkistointi.support.ArchiveSchedulerTestControl
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.cache.CacheManager
import javax.imageio.ImageIO
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

/**
 * Real approval, PostgreSQL snapshots, production scheduler, Turku adapter, ledger and HTTP client for every
 * Turku graduation variant. The approval ITs replace the adapter with a stub, so this is the only test that feeds
 * the payload produced by a real graduation approval through the real Dynasty mapper and client.
 */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///dynastyGraduationDelivery?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.turku.kaytossa=true",
    "db-scheduler.heartbeat-interval=1h",
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
    "application.arkistointi.turku.dynasty.tutkintoasetukset[test]=Test regulation"
])
@Import(DynastyGraduationDeliveryIT.CacheConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DynastyGraduationDeliveryIT : AsyncArchiveItSupport() {
    @Autowired private lateinit var properties: ApplicationProperties
    @Autowired private lateinit var client: DynastyClient
    @MockitoSpyBean private lateinit var processor: ArkistointiJobProcessorImpl
    @MockitoBean private lateinit var mail: MailService
    private lateinit var schedulerControl: ArchiveSchedulerTestControl
    private val stepFixture get() = DynastyStepJdbcFixture(jdbc)

    @BeforeEach
    fun resetReceiver() {
        ReflectionTestUtils.setField(client, "token", null)
        schedulerControl = ArchiveSchedulerTestControl(scheduler, processor)
        schedulerControl.pollAndAwait()
        server.dispatcher = QueueDispatcher()
        while (server.takeRequest(1, TimeUnit.MILLISECONDS) != null) { /* drop requests of the previous test */ }
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

    @ParameterizedTest(name = "{0} certificate as {1}")
    @CsvSource("EL,pdf", "EHL,pdf", "YEK,pdf", "YEK,png")
    fun `approved graduation reaches Dynasty as case task and three documents in order`(kind: String, format: String) {
        val image = format == "png"
        val fixture = graduationFixture(kind, YliopistoEnum.TURUN_YLIOPISTO, true)
        transactions.executeWithoutResult { _ ->
            val studyRight = em.find(Valmistumispyynto::class.java, fixture.first).opintooikeus!!
            studyRight.asetus?.nimi?.let {
                properties.getArkistointi().getTurku().dynasty.tutkintoasetukset = mapOf(it to "Test regulation")
            }
            studyRight.erikoistuvaLaakari!!.apply {
                laillistamistodistus = AsiakirjaData(data = if (image) certificateImage else certificate)
                laillistamispaivanLiitetiedostonNimi = "certificate.$format"
                laillistamispaivanLiitetiedostonTyyppi = if (image) "image/png" else "application/pdf"
            }
        }
        setSecurityContext(fixture.second, VASTUUHENKILO)
        approveGraduation(fixture.first).andExpect(status().isOk)
        val job = jobs.findByKey("valmistuminen:TURUN_YLIOPISTO:${fixture.first}")!!
        val taskId = java.util.UUID.randomUUID()
        server.enqueueDynastySuccess(documentIds = listOf(1078, 1079, 1080), taskId = taskId)

        schedulerControl.startAttempt()
        awaitDelivered(job.id!!)

        val requests = (1..7).map { server.takeRequest(5, TimeUnit.SECONDS)!! }
        assertThat(requests.map { it.method + " " + it.path!!.substringBefore('?') }).containsExactly(
            "POST /token", "POST /api/v1/cases", "POST /api/v1/cases/415/tasks",
            "POST /api/v1/documents", "POST /api/v1/documents", "POST /api/v1/documents",
            "POST /api/v1/cases/415/state/Archived"
        )
        assertThat(requests[1].body.readUtf8()).contains("$kind Valmistumisen yhteenveto")
        assertThat(requests[2].body.readUtf8()).contains("Hakeminen")
        val summary = requests[3].body.readUtf8()
        val attachment = requests[4].body.readUtf8()
        val savedCertificate = requests[5].body.readUtf8()
        assertThat(summary).contains("Valmistumisen yhteenveto, EL, EHL tai YEK", "PhaseSelector", taskId.toString())
        assertThat(attachment).contains("parentDocumentSelector", "1078").doesNotContain("Laillistustodistus")
        assertThat(savedCertificate).contains("parentDocumentSelector", "1078", "Laillistustodistus",
            "filename=\"certificate.$format\"", "Content-Type: ${if (image) "image/png" else "application/pdf"}")
        assertThat(jobs.findById(job.id!!).orElseThrow().externalId).isEqualTo("415")
        assertThat(stepFixture.steps(job.id!!).associate { it.key to it.status }).isEqualTo(
            listOf("CASE", "TASK", "DOCUMENT:0", "DOCUMENT:1", "DOCUMENT:2", "ARCHIVE_CASE")
                .associateWith { DynastyStepStatus.VALMIS }
        )
        assertThat((0..2).map { stepFixture.step(job.id!!, "DOCUMENT:$it")?.remoteId })
            .containsExactly("1078", "1079", "1080")
    }

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): CacheManager =
            AsyncArchiveItSupport.isolatedArchiveCache("urn:elsa:dynasty-graduation-delivery-it", javaClass.classLoader, customizer)
    }

    companion object {
        private val server = MockWebServer().apply { start() }
        private val certificate = PdfTestData.certificate()
        private val certificateImage = ByteArrayOutputStream().use { output ->
            check(ImageIO.write(BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", output))
            output.toByteArray()
        }

        @JvmStatic
        @DynamicPropertySource
        fun transport(registry: DynamicPropertyRegistry) {
            registry.add("application.arkistointi.turku.dynasty.base-url") { server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/') }
            registry.add("application.arkistointi.turku.dynasty.token-url") { server.url("/token").newBuilder().host("127.0.0.1").build().toString() }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() = server.shutdown()
    }
}

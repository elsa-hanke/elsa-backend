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
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
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
import org.springframework.beans.factory.annotation.Qualifier
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
 "application.arkistointi.turku.metadata.cases.koejakso.documents.arviointi.retention-period=-1",
 "application.arkistointi.turku.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
 "application.arkistointi.turku.metadata.cases.valmistuminen.documents.liite.retention-period=10",
 "application.arkistointi.turku.metadata.cases.valmistuminen.documents.laillistamistodistus.retention-period=10",
 "application.arkistointi.turku.dynasty.client-id=test",
 "application.arkistointi.turku.dynasty.client-secret=test",
 "application.arkistointi.turku.dynasty.organization=0",
 "application.arkistointi.turku.dynasty.case-schema-id=case",
 "application.arkistointi.turku.dynasty.document-schema-id=document",
 "application.arkistointi.turku.dynasty.allow-loopback-http-for-tests=true",
 "application.arkistointi.turku.dynasty.tutkintoasetukset[test]=Test regulation",
 "application.arkistointi.uef.kaytossa=true",
 "application.arkistointi.uef.metadata.cases.koejakso.documents.arviointi.retention-period=-1",
 "application.arkistointi.uef.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
 "application.arkistointi.uef.metadata.cases.valmistuminen.documents.liite.retention-period=10",
 "application.arkistointi.uef.metadata.cases.valmistuminen.documents.laillistamistodistus.retention-period=10",
 "application.arkistointi.uef.dynasty.client-id=test",
 "application.arkistointi.uef.dynasty.client-secret=test",
 "application.arkistointi.uef.dynasty.organization=0",
 "application.arkistointi.uef.dynasty.case-schema-id=uef-case",
 "application.arkistointi.uef.dynasty.document-schema-id=uef-document",
 "application.arkistointi.uef.dynasty.allow-loopback-http-for-tests=true",
 "application.arkistointi.uef.dynasty.tutkintoasetukset[test]=Test regulation"
])
@Import(DynastyDeliveryIT.CacheConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DynastyDeliveryIT : AsyncArchiveItSupport() {

 @Autowired private lateinit var properties: fi.elsapalvelu.elsa.config.ApplicationProperties
 @Autowired @Qualifier("turkuDynastyClient")
 private lateinit var turkuClient: DynastyClient
 @Autowired @Qualifier("uefDynastyClient")
 private lateinit var uefClient: DynastyClient
 @Autowired private lateinit var task: com.github.kagkarlsson.scheduler.task.helper.OneTimeTask<Unit>
 @Autowired private lateinit var dataSource: javax.sql.DataSource
 @MockitoSpyBean private lateinit var processor: ArkistointiJobProcessorImpl
 @MockitoBean lateinit var mail: MailService
    private val stepFixture get() = DynastyStepJdbcFixture(jdbc)
    private lateinit var schedulerControl: ArchiveSchedulerTestControl

    @BeforeEach
    fun resetReceiver() {
        listOf(turkuClient, uefClient).forEach { ReflectionTestUtils.setField(it, "token", null) }
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


    private fun job(
        university: YliopistoEnum = YliopistoEnum.TURUN_YLIOPISTO, accepted: Boolean = true
    ): fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob {
        val id = approveKoejakso(university, accepted)
        return jobs.findByKey("koejakso-arvio:$university:$id")!!
    }
    private fun enqueue(vararg bodies: String) = bodies.forEach { server.enqueue(MockResponse().setBody(it)) }
    @ParameterizedTest(name = "{0}, accepted = {1}")
    @CsvSource("TURUN_YLIOPISTO,true", "TURUN_YLIOPISTO,false", "ITA_SUOMEN_YLIOPISTO,true", "ITA_SUOMEN_YLIOPISTO,false")
    fun `final koejakso decision sends one confidential assessment before archiving the case`(university: YliopistoEnum, accepted: Boolean) {
        val job = job(university, accepted)
        assertThat(objectMapper.readTree(job.payload).path("koejaksoHyvaksytty")).isEqualTo(objectMapper.nodeFactory.booleanNode(accepted))

        val turku = university == YliopistoEnum.TURUN_YLIOPISTO
        val tosFixture = if (turku) DynastyTosFixture.ASSESSMENT else DynastyTosFixture.UEF_ASSESSMENT
        val phaseId = UUID.randomUUID()
        tosFixture.enqueueSuccess(server, listOf(1078), phaseId)
        val requestsBefore = server.requestCount
        val archiveZone = ZoneId.of("Europe/Helsinki")
        val earliestDeliveryDate = LocalDate.now(archiveZone)

        startAttempt()
        awaitDelivered(job.id!!)

        val requests = assertDeliveryRequests(requestsBefore, expectedDeliveryRequests(tosFixture, phaseId, documentCount = 1))
        val document = requests.single { it.path == "/api/v1/documents" }.body.readUtf8()
        assertKoejaksoDocument(document, turku, phaseId, earliestDeliveryDate, LocalDate.now(archiveZone))
        assertThat(jobs.findById(job.id!!).orElseThrow().externalId).isEqualTo("415")
        val stepKeys = listOf("CASE", "DOCUMENT:0", "ARCHIVE_CASE")
        assertThat(stepFixture.steps(job.id!!).associate { it.key to it.status })
            .isEqualTo(stepKeys.associateWith { DynastyStepStatus.VALMIS })
    }
    @Test fun `success commit failure resumes completed ledger without another HTTP request`() {
        val job = job()
        withSuccessCommitFailure {
            DynastyTosFixture.ASSESSMENT.enqueueSuccess(server, listOf(1078))
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

    @Test fun `manual retry skips completed case after definite document rejection`() {
        val job = job()
        enqueue(DYNASTY_TEST_TOKEN_RESPONSE)
        with(DynastyTosFixture.ASSESSMENT) { server.enqueueLookup() }
        enqueue("415")
        server.enqueue(MockResponse().setResponseCode(403))
        startAttempt()
        awaitParked(job.id!!)
        assertThat(stepFixture.steps(job.id!!).filter { it.status == DynastyStepStatus.VALMIS }.map { it.key })
            .containsExactly("CASE")
        retryParkedJob(job.id!!)
        with(DynastyTosFixture.ASSESSMENT) { server.enqueueLookup() }
        enqueue("1078", "")
        startAttempt()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val result = jobs.findById(job.id!!).orElseThrow()
            assertThat(result.status)
                .describedAs("error=%s steps=%s",result.errorCode,stepFixture.steps(job.id!!))
                .isEqualTo(ArkistointiJobStatus.LAHETETTY)
        }
        val requests = (1..9).map { server.takeRequest(5, TimeUnit.SECONDS)!! }
        assertThat(requests.count { it.path!!.startsWith("/api/v1/cases?") }).isEqualTo(1)
        assertThat(requests.count { it.path!!.contains("/tasks") }).isZero()
    }
    @Test fun `lost case response parks job and retry never creates another case`() {
        val job = job()
        enqueue(DYNASTY_TEST_TOKEN_RESPONSE)
        with(DynastyTosFixture.ASSESSMENT) { server.enqueueLookup() }
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
                DynastyTosFixture.ASSESSMENT.response(request.path.orEmpty())?.let { return it }
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
            assertThat(server.requestCount).isEqualTo(requestsBefore + 4)
            assertThat(stepFixture.steps(job.id!!).count { it.key == "TASK" }).isZero()
        } finally {
            release.countDown()
            recovery.stop()
        }
    }
    @ParameterizedTest(name = "{0}: {1} certificate as {2}")
    @CsvSource(
        "TURUN_YLIOPISTO,EL,pdf", "TURUN_YLIOPISTO,EHL,pdf", "TURUN_YLIOPISTO,YEK,pdf", "TURUN_YLIOPISTO,YEK,png",
        "ITA_SUOMEN_YLIOPISTO,EL,pdf", "ITA_SUOMEN_YLIOPISTO,YEK,png"
    )
    fun `approved valmistuminen reaches Dynasty as a case and three TOS documents in order`(university: YliopistoEnum, kind: String, format: String) {
        val image = format == "png"
        val fixture = createValmistumispyyntoFixture(kind, university, true)
        transactions.executeWithoutResult { _ ->
            val request = em.find(Valmistumispyynto::class.java, fixture.first)
            request.virkailijanKuittausaika = reviewDate
            val opintooikeus = request.opintooikeus!!
            opintooikeus.opintooikeudenMyontamispaiva = studyRightStartDate
            dynastySettings(university).tutkintoasetukset = mapOf(opintooikeus.asetus!!.nimi!! to "Test regulation")
            opintooikeus.erikoistuvaLaakari!!.apply {
                laillistamispaiva = licensingDate
                laillistamistodistus = AsiakirjaData(data = if (image) certificateImage else certificate)
                laillistamispaivanLiitetiedostonNimi = "certificate.$format"
                laillistamispaivanLiitetiedostonTyyppi = if (image) "image/png" else "application/pdf"
            }
        }
        setSecurityContext(fixture.second, VASTUUHENKILO)
        approveValmistumispyynto(fixture.first).andExpect(status().isOk)
        val approvalDate = requireNotNull(transactions.execute {
            em.find(Valmistumispyynto::class.java, fixture.first).vastuuhenkiloHyvaksyjaKuittausaika
        })
        assertThat(listOf(reviewDate, studyRightStartDate, licensingDate, approvalDate)).doesNotHaveDuplicates()
        val job = jobs.findByKey("valmistuminen:$university:${fixture.first}")!!
        val taskId = UUID.randomUUID()
        val turku = university == YliopistoEnum.TURUN_YLIOPISTO
        val documentIds = listOf(1078L, 1079L, 1080L)
        val tosFixture = if (turku) DynastyTosFixture.GRADUATION else DynastyTosFixture.UEF_GRADUATION
        tosFixture.enqueueSuccess(server, documentIds, taskId)
        val requestsBefore = server.requestCount

        startAttempt()
        awaitDelivered(job.id!!)

        val requests = assertDeliveryRequests(requestsBefore, expectedDeliveryRequests(tosFixture, taskId, documentCount = 3))
        val caseBody = requests.single { it.path!!.startsWith("/api/v1/cases?") }.body.readUtf8()
        assertThat(objectMapper.readTree(caseBody).at("/caseModel/title").asText()).startsWith("$kind ")

        val documents = requests.filter { it.path == "/api/v1/documents" }.map { it.body.readUtf8() }
        assertGraduationSelectors(documents, turku, taskId, tosFixture)
        assertGraduationDates(documents, turku, approvalDate)
        assertThat(documents.last()).contains("filename=\"certificate.$format\"", "Content-Type: ${if (image) "image/png" else "application/pdf"}")
        assertThat(jobs.findById(job.id!!).orElseThrow().externalId).isEqualTo("415")
        val stepKeys = listOf("CASE", "DOCUMENT:0", "DOCUMENT:1", "DOCUMENT:2", "ARCHIVE_CASE")
        assertThat(stepFixture.steps(job.id!!).associate { it.key to it.status })
            .isEqualTo(stepKeys.associateWith { DynastyStepStatus.VALMIS })
        assertThat((0..2).map { stepFixture.step(job.id!!, "DOCUMENT:$it")?.remoteId }).containsExactly("1078", "1079", "1080")
    }

    private fun assertGraduationDates(documents: List<String>, turku: Boolean, approvalDate: LocalDate) {
        val metadataPart = if (turku) "metaData" else "MetaData"
        val prefix = if (turku) "UTU" else "UEF"
        val summary = objectMapper.dynastyMultipartJson(documents.first(), metadataPart).path("AD")
        val certificate = objectMapper.dynastyMultipartJson(documents.last(), metadataPart).path("AD")
        assertThat(summary.path("${prefix}valmistumispaiva").asText()).describedAs("summary graduation date")
            .isEqualTo(approvalDate.toString())
        assertThat(summary.path("${prefix}alkamispaiva").asText()).describedAs("summary study right start date")
            .isEqualTo(studyRightStartDate.toString())
        assertThat(summary.path("${prefix}laillistamispaiva").asText()).describedAs("summary licensing date")
            .isEqualTo(licensingDate.toString())
        assertThat(certificate.path("${prefix}laillistamispaiva").asText()).describedAs("certificate licensing date")
            .isEqualTo(licensingDate.toString())
    }

    private fun assertDeliveryRequests(requestsBefore: Int, expected: List<String>): List<RecordedRequest> {
        assertThat(server.requestCount - requestsBefore).isEqualTo(expected.size)
        val requests = List(expected.size) { requireNotNull(server.takeRequest(5, TimeUnit.SECONDS)) }
        assertThat(requests.map { it.method + " " + it.path!!.substringBefore('?') }).containsExactlyElementsOf(expected)
        return requests
    }

    private fun expectedDeliveryRequests(tosFixture: DynastyTosFixture, phaseId: UUID, documentCount: Int): List<String> = buildList {
        add("POST /token")
        add("GET ${tosFixture.path}")
        add("GET ${tosFixture.path}/$phaseId/documenttypes")
        add("POST /api/v1/cases")
        repeat(documentCount) { add("POST /api/v1/documents") }
        add("POST /api/v1/cases/415/state/Archived")
    }

    private fun assertKoejaksoDocument(
        document: String, turku: Boolean, phaseId: UUID, earliestDeliveryDate: LocalDate, latestDeliveryDate: LocalDate
    ) {
        val metadata = objectMapper.dynastyMultipartJson(document, if (turku) "metaData" else "MetaData")
        val options = objectMapper.dynastyMultipartJson(document, if (turku) "caseAttachmentOptions" else "CaseAttachmentOptions")
        val secrecy = metadata.path("SecrecyPeriod")
        val start = LocalDateTime.parse(secrecy.path("Start").asText())
        assertThat(start.toLocalDate()).isBetween(earliestDeliveryDate, latestDeliveryDate)
        assertThat(start.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
        assertThat(LocalDateTime.parse(secrecy.path("End").asText())).isEqualTo(start.plusYears(100))
        assertThat(metadata.path("state").asInt()).isEqualTo(3)
        assertThat(options.at("/CaseSelector/Id").longValue()).isEqualTo(415)
        assertThat(options.at("/PhaseSelector/smartAttachOptions/pathSelector/0/Id").asText()).isEqualTo(phaseId.toString())
        assertThat(options.path("TOSTitle").asText()).isEqualTo("Koejakson vastuuhenkilön arvio")
        assertThat(metadata.has("publicityClassId")).isFalse()
        assertThat(metadata.has("publicityClassName")).isFalse()
        assertThat(metadata.has("retentionPeriodLength")).isFalse()
    }

    private fun assertGraduationSelectors(
        documents: List<String>, turku: Boolean, phaseId: UUID, tosFixture: DynastyTosFixture
    ) {
        val optionsPart = if (turku) "caseAttachmentOptions" else "CaseAttachmentOptions"
        val options = documents.map { objectMapper.dynastyMultipartJson(it, optionsPart) }
        options.forEach {
            assertThat(it.at("/CaseSelector/Id").longValue()).isEqualTo(415)
            assertThat(it.at("/PhaseSelector/smartAttachOptions/pathSelector/0/Id").asText()).isEqualTo(phaseId.toString())
            assertThat(it.has("parentDocumentSelector")).isFalse()
        }
        val titles = tosFixture.documentTitles
        val expectedTitles = if (turku) titles + titles.last() else titles
        assertThat(options.map { it.path("TOSTitle").asText() }).containsExactlyElementsOf(expectedTitles)
    }

    private fun approveKoejakso(university: YliopistoEnum, accepted: Boolean): Long {
        val fixture = koejaksoFixture(university, CaseType.KOEJAKSO)
        transactions.executeWithoutResult {
            val opintooikeus = vastuuhenkilonArvioRepository.findById(fixture.second).orElseThrow().opintooikeus!!
            dynastySettings(university).tutkintoasetukset = mapOf(opintooikeus.asetus!!.nimi!! to "Test regulation")
        }
        approveKoejaksonVastuuhenkilonArvio(fixture, accepted).andExpect(status().isOk)
        return fixture.second
    }

    private fun dynastySettings(university: YliopistoEnum) =
        DynastyArchiveProfile.from(university).archiveProperties(properties.getArkistointi()).dynasty

    private fun startAttempt() = schedulerControl.startAttempt()

    private fun pollAndAwait() = schedulerControl.pollAndAwait()

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): CacheManager =
            AsyncArchiveItSupport.isolatedArchiveCache("urn:elsa:dynasty-delivery-it", javaClass.classLoader, customizer)
    }

    companion object {
        private val reviewDate = LocalDate.of(2026, 9, 7)
        private val studyRightStartDate = LocalDate.of(2020, 8, 1)
        private val licensingDate = LocalDate.of(2019, 6, 14)
        private val server = MockWebServer().apply { start() }
        private val certificate = PdfTestData.certificate()
        private val certificateImage = ByteArrayOutputStream().use { output ->
            check(ImageIO.write(BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", output))
            output.toByteArray()
        }
        @JvmStatic @DynamicPropertySource
        fun transport(registry: DynamicPropertyRegistry) {
            listOf("turku", "uef").forEach { destination ->
                registry.add("application.arkistointi.$destination.dynasty.base-url") {
                    server.url("/").newBuilder().host("127.0.0.1").build().toString().trimEnd('/')
                }
                registry.add("application.arkistointi.$destination.dynasty.token-url") {
                    server.url("/token").newBuilder().host("127.0.0.1").build().toString()
                }
            }
        }
        @JvmStatic @AfterAll fun stopServer() = server.shutdown()
    }
}

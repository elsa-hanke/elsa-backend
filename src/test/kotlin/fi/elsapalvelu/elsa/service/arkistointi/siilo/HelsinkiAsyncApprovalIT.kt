package fi.elsapalvelu.elsa.service.arkistointi.siilo

import com.github.kagkarlsson.scheduler.Scheduler
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintoopasHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyynnonTarkistusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyyntoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import okhttp3.mockwebserver.QueueDispatcher
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.cache.Caching
import javax.xml.parsers.DocumentBuilderFactory

import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKoulutussopimusMapper
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.DynamicPropertyRegistry
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Real HTTP approval, PDFs, immutable snapshots, PostgreSQL, scheduler and Siilo HTTP multipart. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///helsinkiAsyncApproval?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.hki.kaytossa=true",
    "application.arkistointi.tre.kaytossa=false",
    "application.arkistointi.hki.api-key=integration-test",
    "application.arkistointi.hki.metadata.zip-metadata=true",
    "application.arkistointi.hki.metadata.organisation=Helsingin yliopisto",
    "application.arkistointi.hki.metadata.cases.valmistuminen.async-enabled=true",
    "application.arkistointi.hki.metadata.cases.valmistuminen.title=Valmistuminen",
    "application.arkistointi.hki.metadata.cases.valmistuminen.type=Raportti",
    "application.arkistointi.hki.metadata.cases.valmistuminen.siilo-koodi=graduation",
    "application.arkistointi.hki.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
    "application.arkistointi.hki.metadata.cases.koejakso.async-enabled=true",
    "application.arkistointi.hki.metadata.cases.koejakso.title=Koejakson arviointi",
    "application.arkistointi.hki.metadata.cases.koejakso.type=Muu",
    "application.arkistointi.hki.metadata.cases.koejakso.siilo-koodi=assessment",
    "application.arkistointi.hki.metadata.cases.koejakso.documents.arviointi.retention-period=20",
    "application.arkistointi.hki.metadata.cases.sopimus.async-enabled=true",
    "application.arkistointi.hki.metadata.cases.sopimus.title=Koulutuspaikkasopimus",
    "application.arkistointi.hki.metadata.cases.sopimus.type=Sopimus",
    "application.arkistointi.hki.metadata.cases.sopimus.siilo-koodi=contract",
    "application.arkistointi.hki.metadata.cases.sopimus.documents.sopimus.retention-period=20",
    "application.arkistointi.retry.initial-delay=1s",
    "application.arkistointi.retry.max-delay=1s",
    "db-scheduler.enabled=true",
    "db-scheduler.polling-interval=1s",
    "db-scheduler.threads=1"
])
@Import(HelsinkiAsyncApprovalIT.CacheConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HelsinkiAsyncApprovalIT : ResourceIntegrationTestBase() {
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var scheduler: Scheduler
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var assessments: KoejaksonVastuuhenkilonArvioRepository
    @Autowired private lateinit var assessmentMapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired private lateinit var contracts: KoejaksonKoulutussopimusRepository
    @Autowired private lateinit var contractMapper: KoejaksonKoulutussopimusMapper
    @MockitoBean lateinit var mail: MailService

    @BeforeEach
    fun resetReceiver() {
        server.dispatcher = QueueDispatcher()
        while (server.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // Remove requests left by a failed previous assertion.
        }
    }

    @AfterEach
    fun clearPendingExecutions() {
        await().atMost(Duration.ofSeconds(30)).until { scheduler.currentlyExecuting.isEmpty() }
        jdbc.queryForList("select task_instance from scheduled_tasks where task_name = 'arkistointi-job'", String::class.java)
            .forEach { scheduler.cancel(TaskInstanceId.of("arkistointi-job", it)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `graduation approval delivers only Helsinki configured documents`(kind: String) {
        val fixture = graduationFixture(kind)
        setSecurityContext(fixture.second, VASTUUHENKILO)
        testMockMvc.perform(put("/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${fixture.first}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null))).with(csrf()))
            .andExpect(status().isOk)
        assertDelivery("valmistuminen:HELSINGIN_YLIOPISTO:${fixture.first}", RecordType.YHTEENVETO, "graduation")
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `koejakso approval delivers its saved contract or assessment`(type: CaseType) {
        val id = approveKoejakso(type)
        val prefix = if (type == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
        assertDelivery("$prefix:HELSINGIN_YLIOPISTO:$id",
            if (type == CaseType.SOPIMUS) RecordType.SOPIMUS else RecordType.ARVIOINTI,
            if (type == CaseType.SOPIMUS) "contract" else "assessment")
    }

    @Test
    fun `temporary receiver outage preserves committed approval and retries the same ZIP`() {
        val id = approveKoejakso(CaseType.KOEJAKSO)
        val job = jobs.findByKey("koejakso-arvio:HELSINGIN_YLIOPISTO:$id")!!
        server.enqueue(MockResponse().setResponseCode(503).setBody("private response"))
        scheduler.triggerCheckForDueExecutions()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(jdbc.queryForObject("select consecutive_failures from scheduled_tasks where task_instance = ?",
                Int::class.javaObjectType, job.id.toString())).isEqualTo(1)
        }
        assertThat(jobs.findById(job.id!!).orElseThrow().status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
        assertThat(assessments.findById(id).orElseThrow().vastuuhenkiloHyvaksynyt).isTrue()
        val first = zipEntries(receivedZip(server.takeRequest(5, TimeUnit.SECONDS)!!))
        server.enqueue(MockResponse().setResponseCode(200))
        awaitDelivered(job.id!!)
        val retried = zipEntries(receivedZip(server.takeRequest(5, TimeUnit.SECONDS)!!))
        assertThat(retried.keys).isEqualTo(first.keys)
        first.forEach { (name, bytes) -> assertThat(retried.getValue(name)).isEqualTo(bytes) }
    }

    @Test
    fun `permanent receiver rejection ends retries without rolling back approval`() {
        val id = approveKoejakso(CaseType.KOEJAKSO)
        val job = jobs.findByKey("koejakso-arvio:HELSINGIN_YLIOPISTO:$id")!!
        server.enqueue(MockResponse().setResponseCode(400).setBody("private response"))
        scheduler.triggerCheckForDueExecutions()
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(jobs.findById(job.id!!).orElseThrow().status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
            assertThat(taskCount(job.id!!)).isZero()
        }
        assertThat(assessments.findById(id).orElseThrow().vastuuhenkiloHyvaksynyt).isTrue()
        server.takeRequest(5, TimeUnit.SECONDS)!!
    }

    private fun approveKoejakso(type: CaseType): Long {
        val fixture = transactions.execute { _ ->
            val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO)).also(em::persist)
            val university = YliopistoHelper.createReferenceData(em, YliopistoEnum.HELSINGIN_YLIOPISTO)
            val traineeUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(ERIKOISTUVA_LAAKARI)).also(em::persist)
            val trainee = ErikoistuvaLaakariHelper.createEntity(em, user = traineeUser, yliopisto = university)
            val approver = createVastuuhenkiloForKoejakso(trainee, user = user)
            val trainer = KayttajaHelper.createEntity(em).also(em::persist)
            val supervisor = KayttajaHelper.createEntity(em).also(em::persist)
            val phases = KoejaksonVaiheetHelper.persistKoejaksoVaiheet(em, trainee, trainer, supervisor, approver, university)
            phases.koulutussopimus.erikoistuvanAllekirjoitusaika = LocalDate.now()
            val assessment = KoejaksonVaiheetHelper.createVastuuhenkilonArvio(trainee, approver).apply {
                erikoistuvanKuittausaika = LocalDate.now()
                virkailija = KayttajaHelper.createEntity(em).also(em::persist)
                virkailijaHyvaksynyt = true
                virkailijanKuittausaika = LocalDate.now()
            }
            em.persist(assessment)
            // The contract fixture is already approved as a prerequisite of the assessment.
            if (type == CaseType.SOPIMUS) {
                phases.koulutussopimus.vastuuhenkiloHyvaksynyt = false
                phases.koulutussopimus.vastuuhenkilonKuittausaika = null
            }
            em.flush()
            Triple(phases.koulutussopimus.id!!, assessment.id!!, user.id!!)
        }
        setSecurityContext(fixture.third, VASTUUHENKILO)
        val id = if (type == CaseType.SOPIMUS) fixture.first else fixture.second
        val dto = transactions.execute { _ ->
            if (type == CaseType.SOPIMUS) contractMapper.toDto(contracts.findById(id).orElseThrow())
            else assessmentMapper.toDto(assessments.findById(id).orElseThrow()).apply { koejaksoHyvaksytty = true }
        }
        val path = if (type == CaseType.SOPIMUS) "koulutussopimus" else "vastuuhenkilonarvio"
        testMockMvc.perform(put("/api/vastuuhenkilo/koejakso/$path")
            .contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(dto)).with(csrf()))
            .andExpect(status().isOk)
        return id
    }

    private fun assertDelivery(key: String, type: RecordType, code: String) {
        val jobId = transactions.execute { _ ->
            val job = jobs.findByKey(key)!!
            assertThat(job.status).isEqualTo(ArkistointiJobStatus.ODOTTAA)
            assertThat(job.asiakirjat.map { it.asiakirjatyyppi }).containsExactly(type)
            job.id!!
        }
        server.enqueue(MockResponse().setResponseCode(200).setBody("archived"))
        scheduler.triggerCheckForDueExecutions()
        awaitDelivered(jobId)
        val request = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/unisign/elsa/archive/$code")
        assertThat(request.getHeader("X-Api-Key")).isEqualTo("integration-test")
        val entries = zipEntries(receivedZip(request))
        transactions.executeWithoutResult { _ ->
            val job = jobs.findById(jobId).orElseThrow()
            assertThat(entries.keys).containsExactlyInAnyOrderElementsOf(listOf("sahke.xml") + job.asiakirjat.map { "pdf/${it.filename}" })
            val xml = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                .newDocumentBuilder().parse(ByteArrayInputStream(entries.getValue("sahke.xml")))
            val records = xml.getElementsByTagNameNS("*", "Record")
            assertThat(records.length).isEqualTo(1)
            val source = objectMapper.readTree(job.payload)["documents"].single()["sourceDocumentId"].longValue()
            val record = records.item(0) as org.w3c.dom.Element
            assertThat(record.getElementsByTagNameNS("*", "NativeId").item(0).textContent).isEqualTo(source.toString())
            job.asiakirjat.forEach {
                assertThat(it.asiakirja!!.id).isNotEqualTo(source)
                assertThat(ArkistointiChecksum.sha256(entries.getValue("pdf/${it.filename}"))).isEqualTo(it.sha256)
            }
        }
        val requests = server.requestCount
        scheduler.triggerCheckForDueExecutions()
        assertThat(taskCount(jobId)).isZero()
        assertThat(server.requestCount).isEqualTo(requests)
    }

    private fun receivedZip(request: RecordedRequest): ByteArray {
        val body = request.body.readByteArray()
        val boundary = request.getHeader("Content-Type")!!.substringAfter("boundary=")
        val text = String(body, Charsets.ISO_8859_1)
        val start = text.indexOf("\r\n\r\n") + 4
        val end = text.lastIndexOf("\r\n--$boundary")
        assertThat(start).isGreaterThan(3)
        assertThat(end).isGreaterThan(start)
        return body.copyOfRange(start, end)
    }

    private fun zipEntries(bytes: ByteArray): Map<String, ByteArray> = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        buildMap { while (true) { val entry = zip.nextEntry ?: break; put(entry.name, zip.readBytes()) } }
    }

    private fun awaitDelivered(id: UUID) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(jobs.findById(id).orElseThrow().status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
            assertThat(taskCount(id)).isZero()
        }
    }

    private fun taskCount(id: UUID) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_instance = ?", Int::class.javaObjectType, id.toString()
    )!!

    private fun graduationFixture(kind: String): Pair<Long, String> = transactions.execute { _ ->
        val university = YliopistoHelper.createReferenceData(em, YliopistoEnum.HELSINGIN_YLIOPISTO)
        val specialty = em.findAll(Erikoisala::class).first {
            when (kind) {
                "YEK" -> it.id == YEK_ERIKOISALA_ID
                "EHL" -> it.tyyppi == ErikoisalaTyyppi.HAMMASLAAKETIEDE
                else -> it.tyyppi == ErikoisalaTyyppi.LAAKETIEDE && it.id != YEK_ERIKOISALA_ID
            }
        }
        val guide = OpintoopasHelper.createEntity(em, erikoisala = specialty).also(em::persist)
        val trainee = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = university, erikoisala = specialty, opintoopas = guide)
        trainee.laillistamistodistus = AsiakirjaData(data = PdfTestData.certificate())
        em.persist(trainee)
        val studyRight = trainee.getOpintooikeusKaytossa()!!
        em.persist(Koulutussuunnitelma(opintooikeus = studyRight))
        val period = TyoskentelyjaksoHelper.createEntity(em).apply { opintooikeus = studyRight }.also(em::persist)
        val attachment = Asiakirja(nimi = "approval-attachment.pdf", tyyppi = "application/pdf", lisattypvm = LocalDateTime.now(),
            opintooikeus = studyRight, tyoskentelyjakso = period, asiakirjaData = AsiakirjaData(data = PdfTestData.certificate()))
        em.persist(attachment)
        period.asiakirjat.add(attachment)
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO)).also(em::persist)
        val approver = KayttajaHelper.createEntity(em, user)
        val task = if (kind == "YEK") VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN
            else VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
        approver.yliopistotAndErikoisalat.add(KayttajaYliopistoErikoisala(kayttaja = approver, yliopisto = university,
            erikoisala = specialty, vastuuhenkilonTehtavat = em.findAll(VastuuhenkilonTehtavatyyppi::class)
                .filter { it.nimi == task }.toMutableSet()))
        em.persist(approver)
        val request = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(studyRight, approver, approver).also(em::persist)
        em.persist(ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(request))
        em.flush()
        request.id!! to user.id!!
    }

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:helsinki-async-approval-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }

    companion object {
        private val server = MockWebServer().apply { start() }
        @JvmStatic @DynamicPropertySource
        fun transport(registry: DynamicPropertyRegistry) {
            registry.add("application.arkistointi.hki.host") { server.url("/").toString().trimEnd('/') }
        }
        @JvmStatic @AfterAll fun stopServer() = server.shutdown()
    }
}

package fi.elsapalvelu.elsa.service.arkistointi.support

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
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
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKoulutussopimusMapper
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
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.cache.CacheManager
import javax.cache.Caching
import javax.xml.parsers.DocumentBuilderFactory
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.transaction.support.TransactionTemplate


/** Shared approval actions, persisted fixtures and archive job assertions. */
// Approval ITs explicitly trigger due checks and use one scheduler worker.
// Retry delays and transport-specific barriers are configured by each concrete test.
@TestPropertySource(properties = [
    "db-scheduler.enabled=true",
    "db-scheduler.polling-interval=1h",
    "db-scheduler.threads=1"
])
open class AsyncArchiveItSupport : ResourceIntegrationTestBase() {
    @Autowired protected lateinit var transactions: TransactionTemplate
    @Autowired protected lateinit var jobs: ArkistointiJobRepository
    @Autowired protected lateinit var jdbc: JdbcTemplate

    @Autowired protected lateinit var scheduler: Scheduler
    @Autowired protected lateinit var assessments: KoejaksonVastuuhenkilonArvioRepository
    @Autowired protected lateinit var assessmentMapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired private lateinit var contracts: KoejaksonKoulutussopimusRepository
    @Autowired private lateinit var contractMapper: KoejaksonKoulutussopimusMapper

    protected fun approveGraduation(id: Long) = approvalRequest(
        "/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/$id",
        ValmistumispyyntoHyvaksyntaFormDTO(null)
    )

    protected fun approveAssessment(fixture: Triple<Long, Long, String>, accepted: Boolean = true, correction: String? = null): ResultActions {
        setSecurityContext(fixture.third, VASTUUHENKILO)
        val dto = transactions.execute {
            assessmentMapper.toDto(assessments.findById(fixture.second).orElseThrow()).apply {
                koejaksoHyvaksytty = accepted
                vastuuhenkilonKorjausehdotus = correction
            }
        }
        return approvalRequest("/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio", requireNotNull(dto))
    }

    protected fun approveContract(fixture: Triple<Long, Long, String>): ResultActions {
        setSecurityContext(fixture.third, VASTUUHENKILO)
        val dto = transactions.execute { contractMapper.toDto(contracts.findById(fixture.first).orElseThrow()) }
        return approvalRequest("/api/vastuuhenkilo/koejakso/koulutussopimus", requireNotNull(dto))
    }

    private fun approvalRequest(path: String, dto: Any) = testMockMvc.perform(
        put(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(dto))
            .with(csrf())
    )

    protected fun awaitParked(id: UUID) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(scheduler.currentlyExecuting).isEmpty()
            assertThat(jobs.findById(id).orElseThrow().status).isEqualTo(ArkistointiJobStatus.VAATII_TOIMENPITEITA)
            val task = jdbc.queryForMap("select picked, execution_time from scheduled_tasks where task_name='arkistointi-job' and task_instance=?", id.toString())
            assertThat(task["picked"]).isEqualTo(false)
            assertThat((task["execution_time"] as Timestamp).toInstant())
                .isEqualTo(Schedule.NEVER)
        }
    }

    protected fun retryParkedJob(id: UUID) {
        val sql = Files.readString(Path.of("ops/arkistointi/retry-job.sql"))
            .replace(":'job_id'", "?")
        assertThat(transactions.execute { jdbc.queryForList(sql, id.toString()) }).hasSize(1)
    }

    protected fun graduationFixture(kind: String, universityType: YliopistoEnum, withCertificate: Boolean): Pair<Long, String> = transactions.execute { _ ->
        val university = YliopistoHelper.createReferenceData(em, universityType)
        val specialty = em.findAll(Erikoisala::class).first {
            when (kind) {
                "YEK" -> it.id == YEK_ERIKOISALA_ID
                "EHL" -> it.tyyppi == ErikoisalaTyyppi.HAMMASLAAKETIEDE
                else -> it.tyyppi == ErikoisalaTyyppi.LAAKETIEDE && it.id != YEK_ERIKOISALA_ID
            }
        }
        val guide = OpintoopasHelper.createEntity(em, erikoisala = specialty).also(em::persist)
        val trainee = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = university, erikoisala = specialty, opintoopas = guide)
        trainee.laillistamistodistus = if (withCertificate) AsiakirjaData(data = PdfTestData.certificate()) else null
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

    protected fun koejaksoFixture(universityType: YliopistoEnum, type: CaseType): Triple<Long, Long, String> =
        transactions.execute { _ ->
            val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO)).also(em::persist)
            val university = YliopistoHelper.createReferenceData(em, universityType)
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

    protected fun zipEntries(bytes: ByteArray): Map<String, ByteArray> = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        buildMap {
            while (true) {
                val entry = zip.nextEntry ?: break
                assertThat(containsKey(entry.name)).describedAs("Duplicate ZIP entry").isFalse()
                put(entry.name, zip.readBytes())
            }
        }
    }

    protected fun awaitDelivered(id: UUID) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            assertThat(jobs.findById(id).orElseThrow().status).isEqualTo(ArkistointiJobStatus.LAHETETTY)
            assertThat(taskCount(id)).isZero()
        }
    }

    protected fun taskCount(id: UUID) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_instance = ?", Int::class.javaObjectType, id.toString()
    )!!

    protected fun assertSavedPackage(jobId: UUID, entries: Map<String, ByteArray>) {
        transactions.executeWithoutResult { _ ->
            val job = jobs.findById(jobId).orElseThrow()
            val identities = objectMapper.readTree(job.payload)["documents"]
            assertThat(entries.keys).containsExactlyInAnyOrderElementsOf(
                listOf("sahke.xml") + job.asiakirjat.map { "pdf/${it.filename}" }
            )
            val xml = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
                .newDocumentBuilder().parse(ByteArrayInputStream(entries.getValue("sahke.xml")))
            val records = xml.getElementsByTagNameNS("*", "Record")
            assertThat(records.length).isEqualTo(job.asiakirjat.size)
            val nativeIds = (0 until records.length).map { index ->
                val record = records.item(index) as org.w3c.dom.Element
                record.getElementsByTagNameNS("*", "NativeId").item(0).textContent
            }
            assertThat(nativeIds).containsExactlyElementsOf(identities.map { it["sourceDocumentId"].asText() })
            job.asiakirjat.forEach { document ->
                val snapshotId = document.asiakirja!!.id
                val identity = identities.single { it["snapshotDocumentId"].longValue() == snapshotId }
                assertThat(identity["sourceDocumentId"].longValue()).isNotEqualTo(snapshotId)
                assertThat(ArkistointiChecksum.sha256(entries.getValue("pdf/${document.filename}")))
                    .isEqualTo(document.sha256)
            }
        }
    }

    companion object {
        /** A unique URI prevents one test context from closing another context's cache manager. */
        fun isolatedArchiveCache(uri: String, classLoader: ClassLoader, customizer: JCacheManagerCustomizer): CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider)
                .getCacheManager(URI(uri), DefaultConfiguration(classLoader))
                .also(customizer::customize)
    }

}

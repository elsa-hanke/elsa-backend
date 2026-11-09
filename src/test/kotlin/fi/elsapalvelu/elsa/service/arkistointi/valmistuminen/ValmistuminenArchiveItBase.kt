package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaLoader
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.arkistointi.job.document.toDescriptor
import fi.elsapalvelu.elsa.service.arkistointi.louhi.TampereLouhiService
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import fi.elsapalvelu.elsa.service.valmistuminen.ValmistumispyyntoService
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintoopasHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyynnonTarkistusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyyntoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import jakarta.persistence.EntityManager
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID
import javax.cache.CacheManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.support.TransactionTemplate

/**
 * Real PDFs, SAHKE packages, approval and queue persistence; only email and remote SFTP are mocked.
 *
 * All bean overrides live here so every subclass shares one Spring context.
 */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///graduationArchive?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.tre.kaytossa=true",
    "application.arkistointi.tre.metadata.zip-metadata=true",
    "application.arkistointi.tre.metadata.organisation=TAU",
    "application.arkistointi.tre.metadata.cases.valmistuminen.async-enabled=true",
    "application.arkistointi.tre.metadata.cases.valmistuminen.title=Valmistuminen",
    "application.arkistointi.tre.metadata.cases.valmistuminen.type=Valmistuminen",
    "application.arkistointi.tre.metadata.cases.valmistuminen.function=04.02.05",
    "application.arkistointi.tre.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
    "application.arkistointi.tre.metadata.cases.valmistuminen.documents.liite.retention-period=10",
    "application.arkistointi.hki.kaytossa=true",
    "application.arkistointi.hki.metadata.cases.valmistuminen.async-enabled=true",
    "application.arkistointi.hki.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
    "application.arkistointi.hki.metadata.zip-metadata=true",
    "application.arkistointi.hki.metadata.organisation=Helsingin yliopisto",
    "application.arkistointi.hki.metadata.cases.valmistuminen.title=Valmistumisen yhteenveto",
    "application.arkistointi.hki.metadata.cases.valmistuminen.type=Raportti",
    "application.arkistointi.hki.metadata.cases.valmistuminen.function=04.12",
    "db-scheduler.enabled=false"
])
@Import(ValmistuminenArchiveItBase.CacheConfig::class)
@AutoConfigureMockMvc
open class ValmistuminenArchiveItBase {
    @Autowired protected lateinit var mvc: MockMvc
    @Autowired protected lateinit var em: EntityManager
    @Autowired protected lateinit var transactions: TransactionTemplate
    @Autowired protected lateinit var jdbc: JdbcTemplate
    @Autowired protected lateinit var service: ValmistumispyyntoService
    @Autowired protected lateinit var jobs: ArkistointiJobRepository
    @Autowired protected lateinit var loader: ArkistointiAsiakirjaLoader
    @Autowired protected lateinit var mapper: ObjectMapper
    @Autowired protected lateinit var properties: ApplicationProperties
    @MockitoSpyBean protected lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @MockitoSpyBean protected lateinit var legacyArchive: SynchronousArkistointiService
    @MockitoBean protected lateinit var mail: MailService
    @MockitoBean protected lateinit var transport: TampereLouhiService

    @BeforeEach
    fun cleanUpDeliveredPackages() {
        doAnswer {
            Files.deleteIfExists(Path.of(it.getArgument<String>(0)))
            null
        }.`when`(transport).laheta(any(), any())
    }

    /** The async path freezes the very same generated output that the sync path packages. */
    protected fun assertQueuedDocumentsMatchSources(job: ArkistointiJob, request: Valmistumispyynto) {
        val identities = mapper.readTree(job.payload)["documents"]
        assertThat(identities.isArray).isTrue()
        assertThat(identities.size()).isEqualTo(job.asiakirjat.size)
        job.asiakirjat.forEach { document ->
            val source = when (document.asiakirjatyyppi) {
                RecordType.YHTEENVETO -> request.yhteenvetoAsiakirja!!
                RecordType.LIITE -> request.liitteetAsiakirja!!
                else -> error("Unexpected archive document type")
            }
            val bytes = source.asiakirjaData!!.data!!
            assertThat(loader.load(document.toDescriptor()).sisalto).containsExactly(*bytes)
            assertThat(document.filename).isEqualTo(source.nimi)
            assertThat(document.contentType).isEqualTo(source.tyyppi)
            assertThat(document.sha256).isEqualTo(ArkistointiChecksum.sha256(bytes))
            assertThat(document.asiakirja!!.id).isNotEqualTo(source.id)
            val identity = identities.single { it["snapshotDocumentId"].longValue() == document.asiakirja!!.id }
            assertThat(identity["sourceDocumentId"].longValue()).isEqualTo(source.id).isNotEqualTo(document.asiakirja!!.id)
            assertThat(identity["recordType"].asText()).isEqualTo(document.asiakirjatyyppi!!.name)
            assertThat(identity["jarjestysnumero"].intValue()).isEqualTo(document.jarjestysnumero)
        }
    }

    protected fun assertSnapshots(job: ArkistointiJob): Map<Long, ByteArray> {
        assertThat(job.expectedDocumentCount).isEqualTo(job.asiakirjat.size).isPositive()
        return job.asiakirjat.associate { document ->
            assertThat(document.asiakirja!!.archiveSnapshot).isTrue
            val bytes = loader.load(document.toDescriptor()).sisalto
            assertThat(bytes).isNotEmpty
            assertThat(document.filename).isEqualTo(document.asiakirja!!.nimi)
            assertThat(document.contentType).isEqualTo(document.asiakirja!!.tyyppi)
            assertThat(document.sha256).isEqualTo(ArkistointiChecksum.sha256(bytes))
            document.id!! to bytes
        }
    }

    protected fun assertRolledBack(fixture: ApprovalFixture) {
        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isNull()
            assertThat(request.yhteenvetoAsiakirja).isNull()
            assertThat(request.liitteetAsiakirja).isNull()
            assertThat(request.erikoistujanTiedotAsiakirja).isNull()
        }
        assertThat(jobs.findByKey(key(fixture))).isNull()
        assertThat(snapshotCount(fixture)).isZero()
        assertThat(jdbc.queryForObject("select count(*) from asiakirja where opintooikeus_id = ?", Long::class.javaObjectType, fixture.studyRightId))
            .isEqualTo(1)
        verifyNoEmailSent()
    }

    protected fun verifyNoEmailSent() {
        verify(mail, never()).sendEmailFromTemplate(anyOrNull<String>(), any(), any(), any(), any(), any())
        verify(mail, never()).sendEmailFromTemplate(any<User>(), any(), any(), any(), any(), any())
    }

    protected fun verifyNoLegacyDelivery() {
        verify(legacyArchive, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    protected fun approve(fixture: ApprovalFixture) = service.updateValmistumispyyntoByHyvaksyjaUserId(
        fixture.id, fixture.approverUserId, ValmistumispyyntoHyvaksyntaFormDTO(null)
    )

    protected fun key(fixture: ApprovalFixture) = "valmistuminen:${fixture.university.name}:${fixture.id}"

    protected fun jobCount(fixture: ApprovalFixture) = jdbc.queryForObject(
        "select count(*) from arkistointi_job where job_key = ?", Long::class.javaObjectType, key(fixture)
    )!!

    protected fun allTaskCount() = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)!!

    protected fun taskCount(jobId: UUID) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_name = 'arkistointi-job' and task_instance = ?",
        Long::class.javaObjectType, jobId.toString()
    )!!

    protected fun snapshotCount(fixture: ApprovalFixture) = jdbc.queryForObject(
        "select count(*) from asiakirja where opintooikeus_id = ? and archive_snapshot",
        Long::class.javaObjectType, fixture.studyRightId
    )!!

    protected fun fixture(
        kind: String,
        certificate: ByteArray? = PdfTestData.certificate(),
        attachment: ByteArray = PdfTestData.certificate(),
        contentType: String = MediaType.APPLICATION_PDF_VALUE,
        certificateContentType: String = MediaType.APPLICATION_PDF_VALUE,
        extraAttachments: List<Pair<String, ByteArray>> = emptyList(),
        certificateFilename: String? = null,
        universityName: YliopistoEnum = YliopistoEnum.TAMPEREEN_YLIOPISTO
    ): ApprovalFixture = transactions.execute {
        val university = YliopistoHelper.createReferenceData(em, universityName)
        val erikoisala = em.findAll(Erikoisala::class).first { erikoisala ->
            when (kind) {
                "YEK" -> erikoisala.id == YEK_ERIKOISALA_ID
                "EHL" -> erikoisala.tyyppi == ErikoisalaTyyppi.HAMMASLAAKETIEDE
                else -> erikoisala.tyyppi == ErikoisalaTyyppi.LAAKETIEDE && erikoisala.id != YEK_ERIKOISALA_ID
            }
        }
        val opintoopas = OpintoopasHelper.createEntity(em, erikoisala = erikoisala)
        em.persist(opintoopas)
        val student = ErikoistuvaLaakariHelper.createEntity(
            em, yliopisto = university, erikoisala = erikoisala, opintoopas = opintoopas,
            laillistamistodistusData = certificate ?: PdfTestData.certificate(),
            laillistamistodistusNimi = certificateFilename ?: "laillistamistodistus.${certificateContentType.substringAfter('/')}",
            laillistamistodistusTyyppi = certificateContentType
        )
        if (certificate == null) student.laillistamistodistus = null
        em.persist(student)
        em.persist(Koulutussuunnitelma(opintooikeus = student.getOpintooikeusKaytossa()!!))
        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em).apply {
            opintooikeus = student.getOpintooikeusKaytossa()!!
        }
        em.persist(tyoskentelyjakso)
        val uploaded = Asiakirja(
            nimi = "approval-attachment", tyyppi = contentType, lisattypvm = LocalDateTime.now(),
            opintooikeus = tyoskentelyjakso.opintooikeus, tyoskentelyjakso = tyoskentelyjakso,
            asiakirjaData = AsiakirjaData(data = attachment)
        )
        em.persist(uploaded)
        tyoskentelyjakso.asiakirjat.add(uploaded)
        extraAttachments.forEachIndexed { index, (type, bytes) ->
            val extra = Asiakirja(
                nimi = "approval-extra-$index", tyyppi = type, lisattypvm = LocalDateTime.now(),
                opintooikeus = tyoskentelyjakso.opintooikeus, tyoskentelyjakso = tyoskentelyjakso,
                asiakirjaData = AsiakirjaData(data = bytes)
            )
            em.persist(extra)
            tyoskentelyjakso.asiakirjat.add(extra)
        }
        val approverUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(approverUser)
        val hyvaksyja = KayttajaHelper.createEntity(em, approverUser)
        val task = if (kind == "YEK") VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN
            else VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
        hyvaksyja.yliopistotAndErikoisalat.add(KayttajaYliopistoErikoisala(
            kayttaja = hyvaksyja, yliopisto = university, erikoisala = erikoisala,
            vastuuhenkilonTehtavat = em.findAll(VastuuhenkilonTehtavatyyppi::class).filter { vastuuhenkilonTehtava -> vastuuhenkilonTehtava.nimi == task }.toMutableSet()
        ))
        em.persist(hyvaksyja)
        val request = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            student.getOpintooikeusKaytossa()!!, hyvaksyja, hyvaksyja
        )
        em.persist(request)
        em.persist(ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(request))
        em.flush()
        ApprovalFixture(request.id!!, request.opintooikeus!!.id!!, approverUser.id!!, uploaded.id!!, universityName)
    }

    protected data class ApprovalFixture(
        val id: Long, val studyRightId: Long, val approverUserId: String, val attachmentId: Long,
        val university: YliopistoEnum
    )

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): CacheManager =
            AsyncArchiveItSupport.isolatedArchiveCache("urn:elsa:graduation-archive-it", javaClass.classLoader, customizer)
    }
}

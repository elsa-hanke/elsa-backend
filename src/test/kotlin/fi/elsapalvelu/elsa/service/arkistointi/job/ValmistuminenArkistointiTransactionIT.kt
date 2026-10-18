package fi.elsapalvelu.elsa.service.arkistointi.job

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.service.PdfA2bService
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.impl.valmistuminen.pdf.ItextPdfAssembler
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import fi.elsapalvelu.elsa.service.valmistuminen.PdfService
import fi.elsapalvelu.elsa.service.valmistuminen.ValmistumispyyntoService
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintoopasHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyynnonTarkistusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ValmistumispyyntoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.support.TransactionTemplate
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import java.time.LocalDate
import javax.cache.Caching

/** Uses real approval, snapshots, job persistence and scheduler SQL against an isolated PostgreSQL. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///graduationArchive?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.tre.kaytossa=true",
    "application.arkistointi.tre.metadata.cases.valmistuminen.async-enabled=true",
    "db-scheduler.enabled=false"
])
@Import(ValmistuminenArkistointiTransactionIT.CacheConfig::class)
class ValmistuminenArkistointiTransactionIT {
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var service: ValmistumispyyntoService
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var loader: ArkistointiAsiakirjaLoader
    @Autowired private lateinit var pdfA: PdfA2bService
    @Autowired private lateinit var metrics: PdfGenerationMetricsService
    @Autowired private lateinit var mapper: ObjectMapper
    @Autowired private lateinit var properties: ApplicationProperties
    @MockitoBean private lateinit var pdfService: PdfService
    @MockitoBean private lateinit var mail: MailService
    @MockitoSpyBean private lateinit var legacyArchive: ArkistointiService

    @BeforeEach
    fun pdfFixture() {
        val bytes = ByteArrayOutputStream()
        pdfA.createDocument(bytes).use { document -> document.addNewPage() }
        val pdf = bytes.toByteArray()
        whenever(pdfService.luoPdf(any(), any(), any())).thenAnswer {
            it.getArgument<OutputStream>(2).write(pdf)
        }
        whenever(pdfService.yhdistaAsiakirjat(any(), any())).thenAnswer {
            it.getArgument<OutputStream>(1).write(pdf)
        }
        whenever(pdfService.openAssembler(any())).thenAnswer {
            ItextPdfAssembler(it.getArgument(0), metrics)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `approval commits immutable documents metadata and scheduler task together`(kind: String) {
        val fixture = fixture(kind)
        approve(fixture)
        val key = key(fixture)
        val expectedTypes = mutableListOf(RecordType.YHTEENVETO, RecordType.LIITE)
        if (kind != "YEK") expectedTypes.add(RecordType.LAILLISTAMISTODISTUS)
        val payload = transactions.execute {
            val job = jobs.findByKey(key)!!
            assertThat(em.find(Valmistumispyynto::class.java, fixture.id).vastuuhenkiloHyvaksyjaKuittausaika)
                .isEqualTo(LocalDate.now())
            assertThat(job.asiakirjat.map { asiakirja -> asiakirja.asiakirjatyyppi }).containsExactlyElementsOf(expectedTypes)
            assertThat(job.asiakirjat.map { asiakirja -> asiakirja.jarjestysnumero }).containsExactlyElementsOf(expectedTypes.indices.toList())
            job.asiakirjat.forEach { asiakirja ->
                assertThat(asiakirja.asiakirja!!.archiveSnapshot).isTrue
                assertThat(pdfA.isPdfA2b(loader.load(asiakirja).sisalto)).isTrue
            }
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            val stored = job.payload!!
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val metadata = mapper.readTree(stored)
            assertThat(metadata["valmistumispyyntoId"].asLong()).isEqualTo(fixture.id)
            assertThat(metadata["opintooikeusId"].asLong()).isEqualTo(fixture.studyRightId)
            assertThat(metadata["university"].asText()).isEqualTo("TAMPEREEN_YLIOPISTO")
            assertThat(metadata["yek"].asBoolean()).isEqualTo(kind == "YEK")
            assertThat(metadata["firstName"].asText()).isEqualTo("john")
            assertThat(metadata["lastName"].asText()).isEqualTo("doe")
            assertThat(metadata["traineeName"].asText()).isEqualTo("john doe")
            assertThat(metadata["studentNumber"].asText()).isEqualTo("AAAAAAAAAA")
            assertThat(metadata["birthDate"].asText()).isEqualTo(LocalDate.ofEpochDay(5).toString())
            assertThat(metadata["reviewer"].asText()).isEqualTo("john doe")
            assertThat(metadata["approver"].asText()).isEqualTo("john doe")
            assertThat(metadata["reviewDate"].asText()).isEqualTo(LocalDate.now().toString())
            assertThat(metadata["approvalDate"].asText()).isEqualTo(LocalDate.now().toString())
            assertThat(metadata["specialtyId"].asLong()).isEqualTo(request.opintooikeus!!.erikoisala!!.id)
            assertThat(metadata["specialtyName"].asText()).isEqualTo(request.opintooikeus!!.erikoisala!!.nimi)
            // Change live sources after approval; neither queued metadata nor bytes may follow them.
            request.opintooikeus!!.erikoistuvaLaakari!!.kayttaja!!.user!!.firstName = "Changed"
            request.opintooikeus!!.opiskelijatunnus = "changed-number"
            request.yhteenvetoAsiakirja!!.asiakirjaData!!.data = "changed-source".toByteArray()
            stored
        }
        transactions.executeWithoutResult {
            val stored = jobs.findByKey(key)!!
            assertThat(stored.payload).isEqualTo(payload)
            stored.asiakirjat.forEach { asiakirja ->
                assertThat(pdfA.isPdfA2b(loader.load(asiakirja).sisalto)).isTrue
            }
        }
        assertThrows<BadRequestAlertException> { approve(fixture) }
        assertThat(jdbc.queryForObject("select count(*) from arkistointi_job where job_key = ?", Long::class.javaObjectType, key))
            .isEqualTo(1)
        verify(mail).sendEmailFromTemplate(any<User>(), any(), any(), any(), any(), any())
        verify(mail).sendEmailFromTemplate(anyOrNull<String>(), any(), any(), any(), any(), any())
        verify(legacyArchive, never()).muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any())
        verify(legacyArchive, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `scheduler failure rolls back approval documents job and actual scheduled row`() {
        val fixture = fixture("EL")
        transactions.executeWithoutResult {
            jdbc.execute("""
                CREATE FUNCTION fail_graduation_schedule() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                BEGIN
                    RAISE EXCEPTION 'Simulated scheduler insertion failure' USING ERRCODE = '23514';
                END; ${'$'}${'$'}
            """.trimIndent())
            jdbc.execute("CREATE TRIGGER fail_graduation_schedule BEFORE INSERT ON scheduled_tasks " +
                "FOR EACH ROW EXECUTE FUNCTION fail_graduation_schedule()")
        }
        val taskCountBefore = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)
        try {
            val exception = assertThrows<RuntimeException> { approve(fixture) }
            assertThat(exception).hasStackTraceContaining("Simulated scheduler insertion failure")
            assertRolledBack(fixture)
            assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType))
                .isEqualTo(taskCountBefore)
        } finally {
            transactions.executeWithoutResult {
                jdbc.execute("DROP TRIGGER fail_graduation_schedule ON scheduled_tasks")
                jdbc.execute("DROP FUNCTION fail_graduation_schedule()")
            }
        }
    }

    @Test
    fun `missing mandatory certificate rolls back approval and partially created snapshots`() {
        val fixture = fixture("EL", certificate = null)
        val exception = assertThrows<ArkistointiAsiakirjaException> { approve(fixture) }
        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_MISSING")
        assertRolledBack(fixture)
    }

    @Test
    fun `disabled integration approves without job or snapshots`() {
        val fixture = fixture("EL", certificate = null)
        properties.getArkistointi().getTre().kaytossa = false
        try {
            approve(fixture)
            assertThat(jobs.findByKey(key(fixture))).isNull()
            assertThat(snapshotCount(fixture)).isZero()
        } finally {
            properties.getArkistointi().getTre().kaytossa = true
        }
    }

    @Test
    fun `async disabled preserves the existing synchronous delivery`() {
        val fixture = fixture("EL", certificate = null)
        val case = properties.getArkistointi().getTre().metadata!!.getCaseMetadata(CaseType.VALMISTUMINEN)!!
        case.asyncEnabled = false
        doReturn(fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiResult("/tmp/legacy.zip", null))
            .whenever(legacyArchive).muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any())
        doNothing().whenever(legacyArchive).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        try {
            approve(fixture)
            assertThat(jobs.findByKey(key(fixture))).isNull()
            assertThat(snapshotCount(fixture)).isZero()
        } finally {
            case.asyncEnabled = true
        }
        verify(legacyArchive).laheta(any(), eq("/tmp/legacy.zip"), any(), any(), eq(fixture.id.toString()), anyOrNull())
    }

    private fun assertRolledBack(fixture: ApprovalFixture) {
        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isNull()
            assertThat(request.yhteenvetoAsiakirja).isNull()
            assertThat(request.liitteetAsiakirja).isNull()
        }
        assertThat(jobs.findByKey(key(fixture))).isNull()
        assertThat(snapshotCount(fixture)).isZero()
        assertThat(jdbc.queryForObject("select count(*) from asiakirja where opintooikeus_id = ?", Long::class.javaObjectType, fixture.studyRightId))
            .isZero()
        verify(mail, never()).sendEmailFromTemplate(anyOrNull<String>(), any(), any(), any(), any(), any())
        verify(mail, never()).sendEmailFromTemplate(any<User>(), any(), any(), any(), any(), any())
    }

    private fun approve(fixture: ApprovalFixture) = service.updateValmistumispyyntoByHyvaksyjaUserId(
        fixture.id, fixture.approverUserId, ValmistumispyyntoHyvaksyntaFormDTO(null)
    )

    private fun key(fixture: ApprovalFixture) = "valmistuminen:TAMPEREEN_YLIOPISTO:${fixture.id}"

    private fun taskCount(jobId: Long) = jdbc.queryForObject(
        "select count(*) from scheduled_tasks where task_name = 'arkistointi-job' and task_instance = ?",
        Long::class.javaObjectType, jobId.toString()
    )!!

    private fun snapshotCount(fixture: ApprovalFixture) = jdbc.queryForObject(
        "select count(*) from asiakirja where opintooikeus_id = ? and archive_snapshot",
        Long::class.javaObjectType, fixture.studyRightId
    )!!

    private fun fixture(kind: String, certificate: ByteArray? = PdfTestData.certificate()): ApprovalFixture = transactions.execute {
        val university = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)
        val specialty = em.findAll(Erikoisala::class).first { erikoisala ->
            when (kind) {
                "YEK" -> erikoisala.id == YEK_ERIKOISALA_ID
                "EHL" -> erikoisala.tyyppi == ErikoisalaTyyppi.HAMMASLAAKETIEDE
                else -> erikoisala.tyyppi == ErikoisalaTyyppi.LAAKETIEDE && erikoisala.id != YEK_ERIKOISALA_ID
            }
        }
        val guide = OpintoopasHelper.createEntity(em, erikoisala = specialty)
        em.persist(guide)
        val student = ErikoistuvaLaakariHelper.createEntity(
            em, yliopisto = university, erikoisala = specialty, opintoopas = guide,
            laillistamistodistusData = certificate ?: PdfTestData.certificate()
        )
        if (certificate == null) student.laillistamistodistus = null
        em.persist(student)
        val approverUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(approverUser)
        val approver = KayttajaHelper.createEntity(em, approverUser)
        val task = if (kind == "YEK") VastuuhenkilonTehtavatyyppiEnum.YEK_VALMISTUMINEN
            else VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
        approver.yliopistotAndErikoisalat.add(KayttajaYliopistoErikoisala(
            kayttaja = approver, yliopisto = university, erikoisala = specialty,
            vastuuhenkilonTehtavat = em.findAll(VastuuhenkilonTehtavatyyppi::class).filter { vastuuhenkilonTehtava -> vastuuhenkilonTehtava.nimi == task }.toMutableSet()
        ))
        em.persist(approver)
        val request = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
            student.getOpintooikeusKaytossa()!!, approver, approver
        )
        em.persist(request)
        em.persist(ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(request))
        em.flush()
        ApprovalFixture(request.id!!, request.opintooikeus!!.id!!, approverUser.id!!)
    }

    private data class ApprovalFixture(val id: Long, val studyRightId: Long, val approverUserId: String)

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:graduation-archive-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }
}

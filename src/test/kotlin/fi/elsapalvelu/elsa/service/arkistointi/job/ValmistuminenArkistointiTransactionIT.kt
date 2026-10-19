package fi.elsapalvelu.elsa.service.arkistointi.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
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
import fi.elsapalvelu.elsa.service.arkistointi.louhi.TampereLouhiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
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
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.support.TransactionTemplate
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipFile
import javax.cache.Caching

/** Real PDFs, SAHKE packages, approval and queue persistence; only email and remote SFTP are mocked. */
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
    @Autowired private lateinit var mapper: ObjectMapper
    @Autowired private lateinit var properties: ApplicationProperties
    @MockitoBean private lateinit var mail: MailService
    @MockitoBean private lateinit var transport: TampereLouhiService
    @MockitoSpyBean private lateinit var legacyArchive: ArkistointiService

    @BeforeEach
    fun cleanUpDeliveredPackages() {
        doAnswer {
            Files.deleteIfExists(Path.of(it.getArgument<String>(0)))
            null
        }.`when`(transport).laheta(any(), any())
    }

    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL", "YEK"])
    fun `approval commits immutable documents metadata and scheduler task together`(kind: String) {
        val fixture = fixture(kind, attachment = PdfTestData.cmykJpeg(), contentType = MediaType.IMAGE_JPEG_VALUE)
        approve(fixture)
        val key = key(fixture)
        val expectedTypes = mutableListOf(RecordType.YHTEENVETO, RecordType.LIITE)
        if (kind != "YEK") expectedTypes.add(RecordType.LAILLISTAMISTODISTUS)
        val archivedBytes = mutableMapOf<Long, ByteArray>()
        val payload = transactions.execute {
            val job = jobs.findByKey(key)!!
            assertThat(em.find(Valmistumispyynto::class.java, fixture.id).vastuuhenkiloHyvaksyjaKuittausaika)
                .isEqualTo(LocalDate.now())
            assertThat(job.asiakirjat.map { asiakirja -> asiakirja.asiakirjatyyppi }).containsExactlyElementsOf(expectedTypes)
            assertThat(job.asiakirjat.map { asiakirja -> asiakirja.jarjestysnumero }).containsExactlyElementsOf(expectedTypes.indices.toList())
            archivedBytes.putAll(assertSnapshots(job))
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            val stored = job.payload!!
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            assertThat(pdfA.isPdfA2b(request.liitteetAsiakirja!!.asiakirjaData!!.data)).isFalse
            assertThat(job.asiakirjat.single { document -> document.asiakirjatyyppi == RecordType.LIITE }.asiakirja!!.id)
                .isNotEqualTo(request.liitteetAsiakirja!!.id)
            assertGeneratedSummary(request)
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
            request.liitteetAsiakirja!!.asiakirjaData!!.data = "changed-attachments".toByteArray()
            em.find(Asiakirja::class.java, fixture.attachmentId).asiakirjaData!!.data = "changed-upload".toByteArray()
            stored
        }
        transactions.executeWithoutResult {
            val stored = jobs.findByKey(key)!!
            assertThat(stored.payload).isEqualTo(payload)
            stored.asiakirjat.forEach { asiakirja ->
                assertThat(loader.load(asiakirja).sisalto).containsExactly(*archivedBytes.getValue(asiakirja.id!!))
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

    @ParameterizedTest
    @ValueSource(strings = ["LONG_PDF", "CMYK_JPEG"])
    fun `async disabled approves real PDFs and synchronously delivers the actual package`(attachmentType: String) {
        val jpeg = attachmentType == "CMYK_JPEG"
        val original = if (jpeg) PdfTestData.cmykJpeg() else PdfTestData.unembeddedFontPdf(30)
        val fixture = fixture(
            "EL", certificate = null, attachment = original,
            contentType = if (jpeg) MediaType.IMAGE_JPEG_VALUE else MediaType.APPLICATION_PDF_VALUE
        )
        val case = properties.getArkistointi().getTre().metadata!!.getCaseMetadata(CaseType.VALMISTUMINEN)!!
        case.asyncEnabled = false
        val tasksBefore = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)
        val delivered = mutableMapOf<String, ByteArray>()
        doAnswer {
            val path = Path.of(it.getArgument<String>(0))
            try {
                ZipFile(path.toFile()).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        delivered[entry.name] = zip.getInputStream(entry).use { input -> input.readBytes() }
                    }
                }
            } finally {
                Files.deleteIfExists(path)
            }
            null
        }.`when`(transport).laheta(any(), eq(false))
        try {
            approve(fixture)
            assertThat(jobs.findByKey(key(fixture))).isNull()
            assertThat(snapshotCount(fixture)).isZero()
            assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType))
                .isEqualTo(tasksBefore)
            transactions.executeWithoutResult {
                val request = em.find(Valmistumispyynto::class.java, fixture.id)
                assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
                assertGeneratedSummary(request)
                val attachments = request.liitteetAsiakirja!!
                val bytes = attachments.asiakirjaData!!.data!!
                PdfDocument(PdfReader(ByteArrayInputStream(bytes))).use { pdf ->
                    assertThat(pdf.numberOfPages).isEqualTo(if (jpeg) 1 else 30)
                    if (!jpeg) repeat(30) { index ->
                        assertThat(PdfTextExtractor.getTextFromPage(pdf.getPage(index + 1)))
                            .isEqualTo("Attachment page ${index + 1}")
                    }
                }
                assertThat(delivered.getValue("pdf/${attachments.nimi}")).containsExactly(*bytes)
                assertThat(delivered.getValue("pdf/${request.yhteenvetoAsiakirja!!.nimi}"))
                    .containsExactly(*request.yhteenvetoAsiakirja!!.asiakirjaData!!.data!!)
                assertThat(em.find(Asiakirja::class.java, fixture.attachmentId).asiakirjaData!!.data)
                    .containsExactly(*original)
            }
            assertThat(delivered.keys).hasSize(3).contains("sahke.xml")
            assertThat(String(delivered.getValue("sahke.xml"), Charsets.UTF_8)).contains(fixture.id.toString())
        } finally {
            case.asyncEnabled = true
        }
        verify(legacyArchive).muodostaSahke(any(), any(), eq(fixture.id.toString()), any(), any(), any(), any(), any(), any())
        verify(legacyArchive).laheta(any(), any(), any(), eq(false), eq(fixture.id.toString()), anyOrNull())
        verify(transport).laheta(any(), eq(false))
    }

    private fun assertGeneratedSummary(request: Valmistumispyynto) {
        PdfDocument(PdfReader(ByteArrayInputStream(request.yhteenvetoAsiakirja!!.asiakirjaData!!.data!!))).use { pdf ->
            assertThat(PdfTextExtractor.getTextFromPage(pdf.firstPage)).containsIgnoringCase("john")
        }
    }

    private fun assertSnapshots(job: ArkistointiJob): Map<Long, ByteArray> = job.asiakirjat.associate { document ->
        assertThat(document.asiakirja!!.archiveSnapshot).isTrue
        val bytes = loader.load(document).sisalto
        assertThat(pdfA.isPdfA2b(bytes)).isTrue
        assertThat(document.sha256).isEqualTo(ArkistointiChecksum.sha256(bytes))
        document.id!! to bytes
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
            .isEqualTo(1)
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

    private fun fixture(
        kind: String,
        certificate: ByteArray? = PdfTestData.certificate(),
        attachment: ByteArray = PdfTestData.certificate(),
        contentType: String = MediaType.APPLICATION_PDF_VALUE
    ): ApprovalFixture = transactions.execute {
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
        em.persist(Koulutussuunnitelma(opintooikeus = student.getOpintooikeusKaytossa()!!))
        val period = TyoskentelyjaksoHelper.createEntity(em).apply {
            opintooikeus = student.getOpintooikeusKaytossa()!!
        }
        em.persist(period)
        val uploaded = Asiakirja(
            nimi = "approval-attachment", tyyppi = contentType, lisattypvm = LocalDateTime.now(),
            opintooikeus = period.opintooikeus, tyoskentelyjakso = period,
            asiakirjaData = AsiakirjaData(data = attachment)
        )
        em.persist(uploaded)
        period.asiakirjat.add(uploaded)
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
        ApprovalFixture(request.id!!, request.opintooikeus!!.id!!, approverUser.id!!, uploaded.id!!)
    }

    private data class ApprovalFixture(val id: Long, val studyRightId: Long, val approverUserId: String, val attachmentId: Long)

    @TestConfiguration(proxyBeanMethods = false)
    class CacheConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:graduation-archive-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }
}

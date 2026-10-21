package fi.elsapalvelu.elsa.service.arkistointi.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
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
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import fi.elsapalvelu.elsa.service.ArchiveImageTestData
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
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.support.TransactionTemplate
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
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
@AutoConfigureMockMvc
class ValmistuminenArkistointiTransactionIT {
    @Autowired private lateinit var mvc: MockMvc
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate
    @Autowired private lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var service: ValmistumispyyntoService
    @Autowired private lateinit var jobs: ArkistointiJobRepository
    @Autowired private lateinit var loader: ArkistointiAsiakirjaLoader
    @MockitoSpyBean private lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
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
        val jobId = requireNotNull(transactions.execute { jobs.findByKey(key)?.id })
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
            assertQueuedDocumentsMatchSources(job, request)
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
                assertThat(loader.load(asiakirja.toDescriptor()).sisalto).containsExactly(*archivedBytes.getValue(asiakirja.id!!))
            }
        }
        assertRepeatedApprovalKeepsJob(fixture, jobId)
        assertThat(jdbc.queryForObject("select count(*) from arkistointi_job where job_key = ?", Long::class.javaObjectType, key))
            .isEqualTo(1)
        verify(mail).sendEmailFromTemplate(any<User>(), any(), any(), any(), any(), any())
        verify(mail).sendEmailFromTemplate(anyOrNull<String>(), any(), any(), any(), any(), any())
        verify(legacyArchive, never()).muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any())
        verify(legacyArchive, never()).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @ValueSource(strings = ["scheduler", "manifest"])
    fun `scheduler or deferred manifest failure rolls back the entire approval`(failure: String) {
        val fixture = fixture("EL")
        val triggerBody = if (failure == "scheduler") {
            "RAISE EXCEPTION 'Simulated scheduler insertion failure' USING ERRCODE = '23514';"
        } else {
            """
                INSERT INTO arkistointi_job_asiakirja
                    (job_id, asiakirja_id, asiakirjatyyppi, jarjestys, tiedostonimi, sisaltotyyppi, sha256)
                SELECT job_id, asiakirja_id, asiakirjatyyppi, 99, tiedostonimi, sisaltotyyppi, sha256
                FROM arkistointi_job_asiakirja WHERE job_id = NEW.task_instance::uuid
                ORDER BY id LIMIT 1;
                RETURN NEW;
            """.trimIndent()
        }
        transactions.executeWithoutResult {
            jdbc.execute("""
                CREATE FUNCTION fail_graduation_schedule() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                BEGIN
                    $triggerBody
                END; ${'$'}${'$'}
            """.trimIndent())
            jdbc.execute("CREATE TRIGGER fail_graduation_schedule BEFORE INSERT ON scheduled_tasks " +
                "FOR EACH ROW EXECUTE FUNCTION fail_graduation_schedule()")
        }
        val taskCountBefore = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)
        try {
            val exception = assertThrows<RuntimeException> { approve(fixture) }
            assertThat(exception).hasStackTraceContaining(if (failure == "scheduler")
                "Simulated scheduler insertion failure" else "Archive job document count does not match the manifest")
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

    @ParameterizedTest
    @ValueSource(strings = ["EL", "EHL"])
    fun `missing mandatory certificate rolls back approval and partially created snapshots`(kind: String) {
        val fixture = fixture(kind, certificate = null)
        val tasksBefore = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)
        val exception = assertArchiveDocumentHttpFailure(mvc, fixture.id, fixture.approverUserId,
            400, "ARCHIVE_DOCUMENT_MISSING")
        assertThat(exception.error.code).isEqualTo("ARCHIVE_DOCUMENT_MISSING")
        assertRolledBack(fixture)
        assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)).isEqualTo(tasksBefore)
    }

    @ParameterizedTest
    @ValueSource(strings = ["JPG_ALIAS", "LARGE_JPEG", "LARGE_PNG", "CMYK_JPEG", "SIXTEEN_BIT_PNG", "ORIENTED_JPEG"])
    fun `approval archives the image certificate without changing bytes format or metadata`(imageType: String) {
        val (contentType, original) = certificateImage(imageType)
        val fixture = fixture("EL", certificate = original, certificateContentType = contentType)
        val sourceId = transactions.execute {
            em.find(Valmistumispyynto::class.java, fixture.id).opintooikeus!!.erikoistuvaLaakari!!.laillistamistodistus!!.id!!
        }

        approve(fixture)

        val archived = transactions.execute {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
            assertGeneratedSummary(request)
            val student = request.opintooikeus!!.erikoistuvaLaakari!!
            assertThat(student.laillistamistodistus!!.id).isEqualTo(sourceId)
            assertThat(student.laillistamistodistus!!.data).containsExactly(*original)
            assertThat(student.laillistamispaivanLiitetiedostonNimi).isEqualTo("laillistamistodistus.${contentType.substringAfter('/')}")
            assertThat(student.laillistamispaivanLiitetiedostonTyyppi).isEqualTo(contentType)
            val job = jobs.findByKey(key(fixture))!!
            assertThat(job.asiakirjat.map { document -> document.asiakirjatyyppi })
                .containsExactly(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
            assertSnapshots(job)
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            val certificate = job.asiakirjat.single { document -> document.asiakirjatyyppi == RecordType.LAILLISTAMISTODISTUS }
            assertThat(certificate.filename).isEqualTo(student.laillistamispaivanLiitetiedostonNimi)
            assertThat(certificate.contentType).isEqualTo(contentType)
            assertThat(certificate.asiakirja!!.asiakirjaData!!.id).isNotEqualTo(sourceId)
            val bytes = loader.load(certificate.toDescriptor()).sisalto
            assertThat(bytes).containsExactly(*original)
            certificate.id!! to bytes
        }
        // A later upload must not replace the certificate already approved and queued.
        transactions.executeWithoutResult {
            val student = em.find(Valmistumispyynto::class.java, fixture.id).opintooikeus!!.erikoistuvaLaakari!!
            student.laillistamistodistus!!.data = ArchiveImageTestData.image("png")
            student.laillistamispaivanLiitetiedostonNimi = "replacement.png"
            student.laillistamispaivanLiitetiedostonTyyppi = MediaType.IMAGE_PNG_VALUE
        }
        transactions.executeWithoutResult {
            val stored = jobs.findByKey(key(fixture))!!.asiakirjat.single { document -> document.id == archived.first }
            assertThat(loader.load(stored.toDescriptor()).sisalto).containsExactly(*archived.second)
            assertThat(stored.filename).isEqualTo("laillistamistodistus.${contentType.substringAfter('/')}")
            assertThat(stored.contentType).isEqualTo(contentType)
            assertThat(stored.sha256).isEqualTo(ArkistointiChecksum.sha256(archived.second))
        }
        verify(transport, never()).laheta(any(), any())
    }

    @ParameterizedTest
    @ValueSource(strings = ["EMPTY", "MISSING_FILENAME", "MISSING_CONTENT_TYPE"])
    fun `incomplete certificate rolls back approval documents and queue with a safe HTTP error`(kind: String) {
        val original = ArchiveImageTestData.image("png")
        val contentType = if (kind == "MISSING_CONTENT_TYPE") "" else "image/png"
        val invalid = if (kind == "EMPTY") byteArrayOf() else original
        val fixture = fixture("EL", certificate = invalid, certificateContentType = contentType,
            certificateFilename = if (kind == "MISSING_FILENAME") "" else "$PRIVATE_CERTIFICATE_NAME.png")
        val tasksBefore = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)

        val code = if (kind == "EMPTY") "ARCHIVE_DOCUMENT_EMPTY" else "ARCHIVE_DOCUMENT_INVALID"
        val exception = assertArchiveDocumentHttpFailure(mvc, fixture.id, fixture.approverUserId, 400, code)

        assertThat(exception.error.code).isEqualTo(code)
        assertRolledBack(fixture)
        assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)).isEqualTo(tasksBefore)
        transactions.executeWithoutResult {
            val student = em.find(Valmistumispyynto::class.java, fixture.id).opintooikeus!!.erikoistuvaLaakari!!
            assertThat(student.laillistamistodistus!!.data).containsExactly(*invalid)
            assertThat(student.laillistamispaivanLiitetiedostonTyyppi).isEqualTo(contentType)
        }
        verify(transport, never()).laheta(any(), any())
    }

    @Test
    fun `YEK approval without a certificate queues only the required document set`() {
        val fixture = fixture("YEK", certificate = null)
        approve(fixture)
        transactions.executeWithoutResult {
            val job = jobs.findByKey(key(fixture))!!
            assertThat(job.asiakirjat.map { document -> document.asiakirjatyyppi })
                .containsExactly(RecordType.YHTEENVETO, RecordType.LIITE)
            assertSnapshots(job)
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            assertThat(em.find(Valmistumispyynto::class.java, fixture.id).vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["STORED_DOCUMENT", "PROCESSING_FAILURE"])
    fun `server document errors return safe 500 and roll back approval`(category: String) {
        val fixture = fixture("EL", certificateFilename = "$PRIVATE_CERTIFICATE_NAME.pdf")
        val tasksBefore = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)
        if (category == "STORED_DOCUMENT") {
            doAnswer { invocation ->
                (invocation.callRealMethod() as ArkistointiAsiakirjaReference).copy(sha256 = "0".repeat(64))
            }.`when`(snapshots).createLaillistamistodistusReference(any(), any())
        } else {
            doThrow(ArkistointiAsiakirjaException(
                ArkistointiError.create("ARCHIVE_DOCUMENT_PROCESSING_FAILED", PRIVATE_CERTIFICATE_NAME),
                ArkistointiAsiakirjaErrorCategory.PROCESSING_FAILURE
            ).apply { addSuppressed(IllegalStateException(PRIVATE_CERTIFICATE_NAME)) })
                .`when`(snapshots).createLaillistamistodistusReference(any(), any())
        }
        val code = if (category == "STORED_DOCUMENT") "ARCHIVE_DOCUMENT_CHECKSUM_MISMATCH" else "ARCHIVE_DOCUMENT_PROCESSING_FAILED"

        val exception = assertArchiveDocumentHttpFailure(mvc, fixture.id, fixture.approverUserId, 500, code)

        assertThat(exception.category).isEqualTo(ArkistointiAsiakirjaErrorCategory.valueOf(category))
        assertRolledBack(fixture)
        assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.javaObjectType)).isEqualTo(tasksBefore)
        verify(transport, never()).laheta(any(), any())
    }

    private fun certificateImage(imageType: String): Pair<String, ByteArray> = when (imageType) {
        "JPG_ALIAS" -> "image/jpg" to ArchiveImageTestData.image()
        "LARGE_JPEG" -> "image/jpeg" to ArchiveImageTestData.image("jpeg", 4000, 3000)
        "LARGE_PNG" -> "image/png" to ArchiveImageTestData.image("png", 4000, 3000)
        "CMYK_JPEG" -> "image/jpeg" to PdfTestData.cmykJpeg()
        "SIXTEEN_BIT_PNG" -> "image/png" to ArchiveImageTestData.sixteenBitPng()
        "ORIENTED_JPEG" -> "image/jpeg" to ArchiveImageTestData.orientedJpeg(6)
        else -> error("Unknown test certificate type")
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
    @ValueSource(strings = ["PDF_30", "PDF_100", "PDF_201", "CMYK_JPEG"])
    fun `async disabled approves real PDFs and synchronously delivers the actual package`(attachmentType: String) {
        val jpeg = attachmentType == "CMYK_JPEG"
        val pages = if (jpeg) 1 else attachmentType.substringAfter('_').toInt()
        val original = if (jpeg) PdfTestData.cmykJpeg() else PdfTestData.unembeddedFontPdf(pages)
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
                    assertThat(pdf.numberOfPages).isEqualTo(pages)
                    if (!jpeg) repeat(pages) { index ->
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

    @ParameterizedTest
    @ValueSource(ints = [30, 100, 201])
    fun `async approval freezes long PDF attachments without losing pages or text`(pages: Int) {
        val original = PdfTestData.unembeddedFontPdf(pages)
        val fixture = fixture("EL", attachment = original)

        approve(fixture)

        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val job = jobs.findByKey(key(fixture))!!
            assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(LocalDate.now())
            assertQueuedDocumentsMatchSources(job, request)
            assertSnapshots(job)
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            val archived = loader.load(job.asiakirjat.single { document -> document.asiakirjatyyppi == RecordType.LIITE }.toDescriptor()).sisalto
            assertAttachmentPdf(archived, pages)
            assertThat(em.find(Asiakirja::class.java, fixture.attachmentId).asiakirjaData!!.data).containsExactly(*original)
        }
        verify(transport, never()).laheta(any(), any())
        verify(legacyArchive, never()).muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `async approval freezes the ordinary merge of a long PDF and CMYK image`(embeddedFont: Boolean) {
        val pages = 100
        val original = if (embeddedFont) PdfTestData.embeddedFontPdf(pages) else PdfTestData.unembeddedFontPdf(pages)
        val jpeg = PdfTestData.cmykJpeg()
        val fixture = fixture("EL", attachment = original,
            extraAttachments = listOf(MediaType.IMAGE_JPEG_VALUE to jpeg))

        approve(fixture)

        transactions.executeWithoutResult {
            val request = em.find(Valmistumispyynto::class.java, fixture.id)
            val job = jobs.findByKey(key(fixture))!!
            assertQueuedDocumentsMatchSources(job, request)
            assertThat(taskCount(job.id!!)).isEqualTo(1)
            val archived = loader.load(job.asiakirjat.single { document -> document.asiakirjatyyppi == RecordType.LIITE }.toDescriptor()).sisalto
            assertAttachmentPdf(archived, pages, totalPages = pages + 1,
                prefix = if (embeddedFont) "Embedded page" else "Attachment page")
            PdfDocument(PdfReader(ByteArrayInputStream(archived))).use { pdf ->
                val images = pdf.getPage(pages + 1).resources.getResource(PdfName.XObject)
                val image = images.getAsStream(images.keySet().single())
                assertThat(image.getAsName(PdfName.ColorSpace)).isEqualTo(PdfName.DeviceCMYK)
            }
        }
        verify(transport, never()).laheta(any(), any())
    }

    /** The async path freezes the very same generated output that the sync path packages. */
    private fun assertQueuedDocumentsMatchSources(job: ArkistointiJob, request: Valmistumispyynto) {
        job.asiakirjat.forEach { document ->
            val student = request.opintooikeus!!.erikoistuvaLaakari!!
            val source = when (document.asiakirjatyyppi) {
                RecordType.YHTEENVETO -> request.yhteenvetoAsiakirja
                RecordType.LIITE -> request.liitteetAsiakirja
                RecordType.LAILLISTAMISTODISTUS -> null
                else -> error("Unexpected archive document type")
            }
            if (document.asiakirjatyyppi != RecordType.LAILLISTAMISTODISTUS) assertThat(source).isNotNull
            val bytes = if (source == null) student.laillistamistodistus!!.data!! else source.asiakirjaData!!.data!!
            val filename = source?.nimi ?: student.laillistamispaivanLiitetiedostonNimi
            val contentType = source?.tyyppi ?: student.laillistamispaivanLiitetiedostonTyyppi
            assertThat(loader.load(document.toDescriptor()).sisalto).containsExactly(*bytes)
            assertThat(document.filename).isEqualTo(filename)
            assertThat(document.contentType).isEqualTo(contentType)
            assertThat(document.sha256).isEqualTo(ArkistointiChecksum.sha256(bytes))
            if (source != null) assertThat(document.asiakirja!!.id).isNotEqualTo(source.id)
        }
    }

    private fun assertAttachmentPdf(bytes: ByteArray, textPages: Int, totalPages: Int = textPages, prefix: String = "Attachment page") {
        PdfDocument(PdfReader(ByteArrayInputStream(bytes))).use { pdf ->
            assertThat(pdf.numberOfPages).isEqualTo(totalPages)
            repeat(textPages) { index ->
                assertThat(PdfTextExtractor.getTextFromPage(pdf.getPage(index + 1))).isEqualTo("$prefix ${index + 1}")
            }
        }
    }

    private fun assertGeneratedSummary(request: Valmistumispyynto) {
        PdfDocument(PdfReader(ByteArrayInputStream(request.yhteenvetoAsiakirja!!.asiakirjaData!!.data!!))).use { pdf ->
            assertThat(PdfTextExtractor.getTextFromPage(pdf.firstPage)).containsIgnoringCase("john")
        }
    }

    private fun assertSnapshots(job: ArkistointiJob): Map<Long, ByteArray> {
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

    private fun assertRepeatedApprovalKeepsJob(fixture: ApprovalFixture, jobId: UUID) {
        val before = transactions.execute {
            val job = jobs.findById(jobId).orElseThrow()
            Triple(job.payload, job.expectedDocumentCount, job.asiakirjat.map { document -> document.id })
        }
        val snapshotsBefore = snapshotCount(fixture)
        assertThrows<BadRequestAlertException> { approve(fixture) }
        assertThat(transactions.execute { jobs.findByKey(key(fixture))?.id }).isEqualTo(jobId)
        assertThat(transactions.execute {
            val job = jobs.findById(jobId).orElseThrow()
            Triple(job.payload, job.expectedDocumentCount, job.asiakirjat.map { document -> document.id })
        }).isEqualTo(before)
        assertThat(snapshotCount(fixture)).isEqualTo(snapshotsBefore)
        assertThat(taskCount(jobId)).isEqualTo(1)
    }

    private fun assertRolledBack(fixture: ApprovalFixture) {
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
        verify(mail, never()).sendEmailFromTemplate(anyOrNull<String>(), any(), any(), any(), any(), any())
        verify(mail, never()).sendEmailFromTemplate(any<User>(), any(), any(), any(), any(), any())
    }

    private fun approve(fixture: ApprovalFixture) = service.updateValmistumispyyntoByHyvaksyjaUserId(
        fixture.id, fixture.approverUserId, ValmistumispyyntoHyvaksyntaFormDTO(null)
    )

    private fun key(fixture: ApprovalFixture) = "valmistuminen:TAMPEREEN_YLIOPISTO:${fixture.id}"

    private fun taskCount(jobId: UUID) = jdbc.queryForObject(
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
        contentType: String = MediaType.APPLICATION_PDF_VALUE,
        certificateContentType: String = MediaType.APPLICATION_PDF_VALUE,
        extraAttachments: List<Pair<String, ByteArray>> = emptyList(),
        certificateFilename: String? = null
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
            laillistamistodistusData = certificate ?: PdfTestData.certificate(),
            laillistamistodistusNimi = certificateFilename ?: "laillistamistodistus.${certificateContentType.substringAfter('/')}",
            laillistamistodistusTyyppi = certificateContentType
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
        extraAttachments.forEachIndexed { index, (type, bytes) ->
            val extra = Asiakirja(
                nimi = "approval-extra-$index", tyyppi = type, lisattypvm = LocalDateTime.now(),
                opintooikeus = period.opintooikeus, tyoskentelyjakso = period,
                asiakirjaData = AsiakirjaData(data = bytes)
            )
            em.persist(extra)
            period.asiakirjat.add(extra)
        }
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

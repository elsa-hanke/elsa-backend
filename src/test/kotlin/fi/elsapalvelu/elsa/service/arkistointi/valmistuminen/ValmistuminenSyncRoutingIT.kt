package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.pdf.PdfTestData
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.zip.ZipFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.http.MediaType

/** Routing away from the async queue: archiving disabled, or async disabled so the legacy sync package is sent. */
class ValmistuminenSyncRoutingIT : ValmistuminenArchiveItBase() {

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
        val tasksBefore = allTaskCount()
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
            assertThat(allTaskCount()).isEqualTo(tasksBefore)
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
}

package fi.elsapalvelu.elsa.service

import com.itextpdf.kernel.pdf.PdfAConformanceLevel
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.pdfa.PdfADocument
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfA2bServiceTest {

    private val service = PdfA2bService(ClassPathResource("sRGB_CS_profile.icm"))

    @Test
    fun `normalizes a regular PDF to PDF-A 2b`() {
        val normalized = service.normalize(fixture("valid.pdf"), MediaType.APPLICATION_PDF_VALUE)

        PdfDocument(PdfReader(ByteArrayInputStream(normalized))).use { document ->
            assertThat(document.reader.pdfAConformanceLevel)
                .isEqualTo(PdfAConformanceLevel.PDF_A_2B)
        }
        PdfADocument(
            PdfReader(ByteArrayInputStream(normalized)),
            PdfWriter(ByteArrayOutputStream())
        ).use { assertThat(it.numberOfPages).isPositive() }
        assertThat(service.isPdfA2b(normalized)).isTrue
    }

    @Test
    fun `normalizes a supported image to PDF-A 2b`() {
        val normalized = service.normalize(fixture("valid.jpg"), MediaType.IMAGE_JPEG_VALUE)

        assertThat(service.isPdfA2b(normalized)).isTrue
    }

    @Test
    fun `rejects corrupt and unsupported content`() {
        assertThatIllegalArgumentException()
            .isThrownBy { service.normalize("not-a-pdf".toByteArray(), MediaType.APPLICATION_PDF_VALUE) }
        assertThatIllegalArgumentException()
            .isThrownBy { service.normalize("document".toByteArray(), MediaType.TEXT_PLAIN_VALUE) }
    }

    @Test
    fun `does not accept a regular PDF as PDF-A 2b without normalization`() {
        assertThat(service.isPdfA2b(fixture("valid.pdf"))).isFalse
    }

    @Test
    fun `rejects a declared PDF-A document with an unembedded font`() {
        val valid = service.normalize(fixture("valid.jpg"), MediaType.IMAGE_JPEG_VALUE)
        val output = ByteArrayOutputStream()
        PdfDocument(PdfReader(ByteArrayInputStream(valid)), PdfWriter(output)).use { document ->
            PdfCanvas(document.getFirstPage()).beginText()
                .setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA), 12F)
                .moveText(10.0, 10.0).showText("Unembedded font").endText()
        }
        assertThat(service.isPdfA2b(output.toByteArray())).isFalse
        assertThat(service.isPdfA2b(service.normalize(output.toByteArray(), MediaType.APPLICATION_PDF_VALUE))).isTrue
    }

    @Test
    fun `checks unchanged content streams instead of trusting PDF-A metadata`() {
        val valid = service.normalize(fixture("valid.jpg"), MediaType.IMAGE_JPEG_VALUE)
        val output = ByteArrayOutputStream()
        PdfDocument(PdfReader(ByteArrayInputStream(valid)), PdfWriter(output)).use { document ->
            document.getFirstPage().put(
                PdfName.Contents,
                PdfStream(("(" + "x".repeat(40_000) + ") Tj").toByteArray())
            )
        }
        assertThat(service.isPdfA2b(output.toByteArray())).isFalse
    }

    @Test
    fun `normalizes permission restricted certificates without changing the original`() {
        val original = PdfTestData.certificate(openingPassword = "")
        val normalized = service.normalize(original, MediaType.APPLICATION_PDF_VALUE)
        assertThat(service.isPdfA2b(normalized)).isTrue
        org.apache.pdfbox.Loader.loadPDF(original).use { assertThat(it.isEncrypted).isTrue }
        assertThatIllegalArgumentException().isThrownBy {
            service.normalize(PdfTestData.certificate(openingPassword = "secret"), MediaType.APPLICATION_PDF_VALUE)
        }
    }

    @Test
    fun `rejects oversized raster pages before allocating an image`() {
        val output = ByteArrayOutputStream()
        PdfDocument(PdfWriter(output)).use { document ->
            val page = document.addNewPage(com.itextpdf.kernel.geom.PageSize(14_400F, 14_400F))
            PdfCanvas(page).beginText().setFontAndSize(PdfFontFactory.createFont(StandardFonts.HELVETICA), 12F)
                .moveText(10.0, 10.0).showText("Force raster fallback").endText()
        }
        assertThatIllegalArgumentException().isThrownBy {
            service.normalize(output.toByteArray(), MediaType.APPLICATION_PDF_VALUE)
        }
    }

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }
}

package fi.elsapalvelu.elsa.service

import com.itextpdf.kernel.pdf.PdfAConformanceLevel
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
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

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }
}

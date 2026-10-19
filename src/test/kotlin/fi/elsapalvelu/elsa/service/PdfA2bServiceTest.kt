package fi.elsapalvelu.elsa.service

import com.itextpdf.kernel.pdf.PdfAConformanceLevel
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.pdf.EncryptionConstants
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.annot.PdfAnnotation
import com.itextpdf.kernel.pdf.annot.PdfSquareAnnotation
import com.itextpdf.kernel.pdf.colorspace.PdfPattern
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState
import com.itextpdf.kernel.colors.PatternColor
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
        val normalized = service.normalize(ArchiveImageTestData.image(), MediaType.IMAGE_JPEG_VALUE)

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
        val valid = service.normalize(ArchiveImageTestData.image(), MediaType.IMAGE_JPEG_VALUE)
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
        val valid = service.normalize(ArchiveImageTestData.image(), MediaType.IMAGE_JPEG_VALUE)
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

    @Test
    fun `removes encryption even when the source still declares PDF-A 2b`() {
        val valid = service.normalize(ArchiveImageTestData.image(), MediaType.IMAGE_JPEG_VALUE)
        val output = ByteArrayOutputStream()
        val properties = WriterProperties().setStandardEncryption(
            byteArrayOf(), "owner".toByteArray(), EncryptionConstants.ALLOW_PRINTING,
            EncryptionConstants.ENCRYPTION_AES_128
        )
        PdfDocument(PdfReader(ByteArrayInputStream(valid)), PdfWriter(output, properties)).use { }
        val encrypted = output.toByteArray()
        PdfDocument(PdfReader(ByteArrayInputStream(encrypted))).use {
            assertThat(it.reader.isEncrypted).isTrue
            assertThat(it.reader.pdfAConformanceLevel).isEqualTo(PdfAConformanceLevel.PDF_A_2B)
        }

        assertThat(service.isPdfA2b(encrypted)).isFalse
        val normalized = service.normalize(encrypted, MediaType.APPLICATION_PDF_VALUE)
        assertThat(service.isPdfA2b(normalized)).isTrue
        PdfDocument(PdfReader(ByteArrayInputStream(normalized))).use {
            assertThat(it.reader.isEncrypted).isFalse
        }
    }

    @Test
    fun `rejects CMYK painting with only an RGB output intent and normalizes it`() {
        val invalid = declaredPdfAWithContent { document ->
            PdfCanvas(document.firstPage).writeLiteral("0 1 1 0 k 0 0 10 10 re f\n")
        }

        assertThat(service.isPdfA2b(invalid)).isFalse
        val normalized = service.normalize(invalid, MediaType.APPLICATION_PDF_VALUE)
        assertThat(normalized).isNotEqualTo(invalid)
        assertThat(service.isPdfA2b(normalized)).isTrue
    }

    @Test
    fun `checks painting inside nested form XObjects`() {
        val invalid = declaredPdfAWithContent { document ->
            val form = PdfFormXObject(Rectangle(10F, 10F))
            PdfCanvas(form, document).writeLiteral("0 1 1 0 K 0 0 10 10 re S\n")
            PdfCanvas(document.firstPage).addXObjectAt(form, 0F, 0F)
        }
        assertThat(service.isPdfA2b(invalid)).isFalse
    }

    @Test
    fun `checks colors inside tiling patterns`() {
        for (color in listOf("1 0 0 rg", "0 1 1 0 k")) {
            val pdf = declaredPdfAWithContent { document ->
                val pattern = PdfPattern.Tiling(10F, 10F)
                PdfCanvas(pattern.pdfObject as PdfStream, pattern.resources, document)
                    .writeLiteral("$color 0 0 10 10 re f\n")
                PdfCanvas(document.firstPage).setFillColor(PatternColor(pattern))
                    .rectangle(0.0, 0.0, 10.0, 10.0).fill()
            }
            assertThat(service.isPdfA2b(pdf)).isEqualTo(color.endsWith("rg"))
        }
    }

    @Test
    fun `checks annotation appearance streams`() {
        for (color in listOf("1 0 0 rg", "0 1 1 0 k")) {
            val pdf = declaredPdfAWithContent { document ->
                val appearance = PdfFormXObject(Rectangle(10F, 10F))
                PdfCanvas(appearance, document).writeLiteral("$color 0 0 10 10 re f\n")
                val annotation = PdfSquareAnnotation(Rectangle(10F, 10F))
                annotation.setFlag(PdfAnnotation.PRINT)
                annotation.setNormalAppearance(appearance.pdfObject)
                document.firstPage.addAnnotation(annotation)
            }
            assertThat(service.isPdfA2b(pdf)).isEqualTo(color.endsWith("rg"))
        }
    }

    @Test
    fun `checks inline image color spaces`() {
        val invalid = declaredPdfAWithContent { document ->
            PdfCanvas(document.firstPage).writeLiteral(
                "q 10 0 0 10 0 0 cm BI /W 1 /H 1 /CS /DeviceCMYK /BPC 8 ID abcd EI Q\n"
            )
        }
        assertThat(service.isPdfA2b(invalid)).isFalse
    }

    @Test
    fun `checks rendering intents and extended graphics states`() {
        val invalidIntent = declaredPdfAWithContent { document ->
            PdfCanvas(document.firstPage).writeLiteral("/InvalidIntent ri 0 0 10 10 re f\n")
        }
        val invalidState = declaredPdfAWithContent { document ->
            val state = PdfExtGState()
            state.pdfObject.put(PdfName.TR, PdfName.Identity)
            PdfCanvas(document.firstPage).setExtGState(state).rectangle(0.0, 0.0, 10.0, 10.0).fill()
        }
        assertThat(service.isPdfA2b(invalidIntent)).isFalse
        assertThat(service.isPdfA2b(invalidState)).isFalse
    }

    @Test
    fun `accepts RGB painting and ignores unused CMYK colors`() {
        val valid = declaredPdfAWithContent { document ->
            PdfCanvas(document.firstPage).writeLiteral("0 1 1 0 k 1 0 0 rg 0 0 10 10 re f\n")
        }
        assertThat(service.isPdfA2b(valid)).isTrue
        assertThat(service.normalize(valid, MediaType.APPLICATION_PDF_VALUE)).containsExactly(*valid)
    }

    @Test
    fun `validates embedded text without rasterizing it`() {
        val normalized = service.normalize(PdfTestData.certificate(), MediaType.APPLICATION_PDF_VALUE)
        assertThat(service.isPdfA2b(normalized)).isTrue
        PdfDocument(PdfReader(ByteArrayInputStream(normalized))).use {
            assertThat(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(it.firstPage))
                .isEqualTo(PdfTestData.CERTIFICATE_TEXT)
        }
        assertThat(service.normalize(normalized, MediaType.APPLICATION_PDF_VALUE)).containsExactly(*normalized)
    }

    private fun declaredPdfAWithContent(edit: (PdfDocument) -> Unit): ByteArray {
        val valid = service.normalize(ArchiveImageTestData.image(), MediaType.IMAGE_JPEG_VALUE)
        val output = ByteArrayOutputStream()
        PdfDocument(PdfReader(ByteArrayInputStream(valid)), PdfWriter(output)).use(edit)
        return output.toByteArray()
    }

    private fun fixture(name: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fixtures/$name")).use { it.readBytes() }
}

package fi.elsapalvelu.elsa.service

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.spy
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayInputStream

class PdfA2bMergeTest {
    private val service = PdfA2bService(ClassPathResource("sRGB_CS_profile.icm"))

    @ParameterizedTest
    @ValueSource(strings = ["unembedded-font", "cmyk", "jpg-alias", "png"])
    fun `normalizes each source without rasterizing neighbouring text`(kind: String) {
        val good = PdfTestData.embeddedFontPdf(30)
        val unchanged = good.copyOf()
        val other = when (kind) {
            "unembedded-font" -> PdfA2bSource(PdfTestData.unembeddedFontPdf(1), "application/pdf")
            "cmyk" -> PdfA2bSource(PdfTestData.cmykJpeg(), "image/jpeg")
            "jpg-alias" -> PdfA2bSource(ArchiveImageTestData.image(), "image/jpg")
            else -> PdfA2bSource(ArchiveImageTestData.image("png"), "image/png")
        }

        val result = service.normalizeAndMerge(sequenceOf(PdfA2bSource(good, "application/pdf"), other))

        assertThat(service.isPdfA2b(result)).isTrue()
        val texts = texts(result)
        assertThat(texts).hasSize(31)
        (1..30).forEach { page -> assertThat(texts[page - 1]).contains("Embedded page $page") }
        assertThat(good).containsExactly(*unchanged)
    }

    @Test
    fun `keeps source order and text when copying restricted PDFs`() {
        val result = service.normalizeAndMerge(sequenceOf(
            PdfA2bSource(PdfTestData.certificate(openingPassword = "", text = "First attachment"), "application/pdf"),
            PdfA2bSource(ArchiveImageTestData.image(), "image/jpeg"),
            PdfA2bSource(PdfTestData.embeddedFontPdf(2, "Last attachment"), "application/pdf")
        ))

        assertThat(service.isPdfA2b(result)).isTrue()
        assertThat(texts(result)).satisfiesExactly(
            { assertThat(it).contains("First attachment") },
            { assertThat(it).isBlank() },
            { assertThat(it).contains("Last attachment 1") },
            { assertThat(it).contains("Last attachment 2") }
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["corrupt", "unsupported", "empty", "missing", "encrypted"])
    fun `rejects an invalid source instead of silently omitting it`(kind: String) {
        val invalid = when (kind) {
            "corrupt" -> PdfA2bSource(byteArrayOf(1, 2, 3), "application/pdf")
            "unsupported" -> PdfA2bSource(byteArrayOf(1), "text/plain")
            "empty" -> PdfA2bSource(byteArrayOf(), "application/pdf")
            "missing" -> PdfA2bSource(null, "application/pdf")
            else -> PdfA2bSource(PdfTestData.certificate(openingPassword = "secret"), "application/pdf")
        }
        assertThatThrownBy {
            service.normalizeAndMerge(sequenceOf(PdfA2bSource(PdfTestData.embeddedFontPdf(1), "application/pdf"), invalid))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `splitting nonconforming PDFs does not reset the total raster budget`() {
        val part = PdfA2bSource(PdfTestData.unembeddedFontPdf(12), "application/pdf")
        assertThatThrownBy { service.normalizeAndMerge(sequenceOf(part, part)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasRootCauseMessage("PDF is too large to rasterize")
    }

    @Test
    fun `a failed final validation does not fall back to rasterizing the merged document`() {
        val validating = spy(service)
        doAnswer {
            val data = it.getArgument<ByteArray>(0)
            if (texts(data).size == 3) false else it.callRealMethod()
        }.`when`(validating).isPdfA2b(anyOrNull())

        assertThatThrownBy {
            validating.normalizeAndMerge(sequenceOf(
                PdfA2bSource(PdfTestData.embeddedFontPdf(1), "application/pdf"),
                PdfA2bSource(PdfTestData.embeddedFontPdf(2), "application/pdf")
            ))
        }.isInstanceOf(IllegalStateException::class.java).hasMessage("Merged archive document is not PDF/A-2b")
    }

    private fun texts(data: ByteArray): List<String> = PdfDocument(PdfReader(ByteArrayInputStream(data))).use { pdf ->
        (1..pdf.numberOfPages).map { PdfTextExtractor.getTextFromPage(pdf.getPage(it)) }
    }
}

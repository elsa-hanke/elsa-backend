package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfAConformanceLevel
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfOutputIntent
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.utils.PdfMerger
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.pdfa.PdfADocument
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

class ItextPdfAssemblerTest {

    private val pdfMetrics = PdfGenerationMetricsService(SimpleMeterRegistry())

    @Test
    fun `assembles all documents into a single PDF in the right order`() {
        val assembler = ItextPdfAssembler(pdf("First"), pdfMetrics)
        assembler.add(pdf("Second"))
        assembler.add(pdf("Third"))

        val result = assembler.finish()

        assertThat(pageCount(result)).isEqualTo(3)
        assertThat(texts(result)).containsExactly("First", "Second", "Third")
    }

    @Test
    fun `the page count grows with every addition`() {
        ItextPdfAssembler(pdf("First"), pdfMetrics).use { assembler ->
            assertThat(assembler.pages).isEqualTo(1)
            assembler.add(pdf("Second"))
            assertThat(assembler.pages).isEqualTo(2)
        }
    }

    @Test
    fun `produces the same result as merging one document at a time`() {
        val documents = (1..5).map { pdf("Page $it") }

        val assembler = ItextPdfAssembler(documents.first(), pdfMetrics)
        documents.drop(1).forEach(assembler::add)
        val assembledResult = assembler.finish()

        var sequentiallyMerged = documents.first()
        documents.drop(1).forEach { sequentiallyMerged = mergeTheOldWay(sequentiallyMerged, it) }

        assertThat(pageCount(assembledResult)).isEqualTo(pageCount(sequentiallyMerged))
        assertThat(texts(assembledResult)).isEqualTo(texts(sequentiallyMerged))
    }

    @Test
    fun `finish is safe to call even if the assembler is already closed`() {
        val assembler = ItextPdfAssembler(pdf("First"), pdfMetrics)
        val result = assembler.finish()

        assembler.close()

        assertThat(pageCount(result)).isEqualTo(1)
    }

    @Test
    fun `smart mode produces the same content as without it`() {
        val documents = (1..12).map { pdfWithFont("Page $it") }

        val smartResult = assemble(documents, smartMode = true)
        val plainResult = assemble(documents, smartMode = false)

        assertThat(pageCount(smartResult)).isEqualTo(pageCount(plainResult))
        assertThat(texts(smartResult)).isEqualTo(texts(plainResult))
    }

    @Test
    fun `smart mode does not copy the same fonts from every source document`() {
        val documents = (1..12).map { pdfWithFont("Page $it") }

        val smartResult = assemble(documents, smartMode = true)
        val plainResult = assemble(documents, smartMode = false)

        // Same content, but shared objects: the assembly has to be clearly smaller.
        assertThat(smartResult.size).isLessThan(plainResult.size / 2)
    }

    // ── PDF/A ────────────────────────────────────────────────────────────────
    //
    // The documents the assembler merges are rendered as PDF/A-2B (see PdfServiceImpl.luoPdf)
    // and the result is archived. The tests above show that smart mode keeps the text; these
    // cover what it could silently lose or collapse while rewriting the object tree of real
    // PDF/A input: the PDF/A identification, the output intent and every distinct page.

    @ParameterizedTest(name = "smartMode={0}")
    @ValueSource(booleans = [true, false])
    fun `the assembled document keeps the PDF_A identification and output intent`(smartMode: Boolean) {
        val documents = (1..5).map { pdfA("Page $it") }
        // Precondition: the input really is PDF/A, otherwise the assertions below prove nothing.
        assertThat(isPdfA(documents.first())).isTrue()
        assertThat(hasOutputIntent(documents.first())).isTrue()

        val result = assemble(documents, smartMode)

        assertThat(isPdfA(result)).withFailMessage("PDF/A identification (XMP pdfaid) was lost").isTrue()
        assertThat(hasOutputIntent(result)).withFailMessage("The output intent was lost").isTrue()
    }

    @ParameterizedTest(name = "smartMode={0}")
    @ValueSource(booleans = [true, false])
    fun `every distinct PDF_A page keeps its own content in order`(smartMode: Boolean) {
        val documents = (1..25).map { pdfA("Page $it") }

        val result = assemble(documents, smartMode)

        // A shared object that wrongly merged two different pages would repeat or drop a text.
        assertThat(texts(result)).containsExactlyElementsOf((1..25).map { "Page $it" })
    }

    @Test
    fun `smart mode and plain mode agree on PDF_A pages and archival metadata`() {
        val documents = (1..25).map { pdfA("Page $it") }

        val smart = assemble(documents, smartMode = true)
        val plain = assemble(documents, smartMode = false)

        assertThat(texts(smart)).isEqualTo(texts(plain))
        assertThat(isPdfA(smart)).isEqualTo(isPdfA(plain))
        assertThat(hasOutputIntent(smart)).isEqualTo(hasOutputIntent(plain))
    }

    private fun assemble(documents: List<ByteArray>, smartMode: Boolean): ByteArray {
        val assembler = ItextPdfAssembler(documents.first(), pdfMetrics, smartMode = smartMode)
        documents.drop(1).forEach(assembler::add)
        return assembler.finish()
    }

    private fun pdf(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        Document(PdfDocument(PdfWriter(out))).use { it.add(Paragraph(text)) }
        return out.toByteArray()
    }

    /**
     * A document with an embedded font, like the real performance entries. Without an embedded
     * font the source documents have nothing significant to share.
     */
    private fun pdfWithFont(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        val font = PdfFontFactory.createFont(
            FONT,
            PdfEncodings.IDENTITY_H,
            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
        )
        // Without subsetting, every source document contains the whole font, the same way real
        // PDF/A documents have a large amount of shareable content.
        font.setSubset(false)
        Document(PdfDocument(PdfWriter(out))).use {
            it.add(Paragraph(text).setFont(font))
        }
        return out.toByteArray()
    }

    /** A PDF/A-2B document with an embedded font, like the ones PdfServiceImpl renders. */
    private fun pdfA(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        val outputIntent = FileInputStream(COLOR_PROFILE).use {
            PdfOutputIntent("Custom", "", "https://www.color.org", "sRGB IEC61966-2.1", it)
        }
        val font = PdfFontFactory.createFont(
            FONT,
            PdfEncodings.IDENTITY_H,
            PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
        )
        Document(PdfADocument(PdfWriter(out), PdfAConformanceLevel.PDF_A_2B, outputIntent)).use {
            it.add(Paragraph(text).setFont(font))
        }
        return out.toByteArray()
    }

    /**
     * The old, quadratically growing implementation that this class replaced: the whole document
     * so far was read and rewritten for every addition. It is kept here, in the test only, as an
     * oracle - the assembler must still produce exactly what it used to.
     */
    private fun mergeTheOldWay(existing: ByteArray, new: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val result = PdfDocument(PdfReader(ByteArrayInputStream(existing)), PdfWriter(out))
        val merger = PdfMerger(result)
        PdfDocument(PdfReader(ByteArrayInputStream(new))).use { source ->
            merger.merge(source, 1, source.numberOfPages)
        }
        result.close()
        return out.toByteArray()
    }

    private fun isPdfA(pdf: ByteArray): Boolean =
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { doc ->
            doc.xmpMetadata?.toString(Charsets.UTF_8)?.contains("pdfaid:part") == true
        }

    private fun hasOutputIntent(pdf: ByteArray): Boolean =
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { doc ->
            (doc.catalog.pdfObject.getAsArray(PdfName.OutputIntents)?.size() ?: 0) > 0
        }

    private fun pageCount(pdf: ByteArray): Int =
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { it.numberOfPages }

    private fun texts(pdf: ByteArray): List<String> =
        PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { doc ->
            (1..doc.numberOfPages).map { page ->
                PdfTextExtractor.getTextFromPage(doc.getPage(page)).trim()
            }
        }

    private companion object {
        const val FONT = "src/main/resources/fonts/LiberationSerif-Regular.ttf"
        const val COLOR_PROFILE = "src/main/resources/sRGB_CS_profile.icm"
    }
}

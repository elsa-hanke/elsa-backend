package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.utils.PdfMerger
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfAssemblerTest {

    private val pdfMetrics = PdfGenerationMetricsService(SimpleMeterRegistry())

    @Test
    fun `assembles all documents into a single PDF in the right order`() {
        val assembler = PdfAssembler(pdf("First"), pdfMetrics)
        assembler.add(pdf("Second"))
        assembler.add(pdf("Third"))

        val result = assembler.finish()

        assertThat(pageCount(result)).isEqualTo(3)
        assertThat(texts(result)).containsExactly("First", "Second", "Third")
    }

    @Test
    fun `the page count grows with every addition`() {
        PdfAssembler(pdf("First"), pdfMetrics).use { assembler ->
            assertThat(assembler.pages).isEqualTo(1)
            assembler.add(pdf("Second"))
            assertThat(assembler.pages).isEqualTo(2)
        }
    }

    @Test
    fun `produces the same result as merging one document at a time`() {
        val documents = (1..5).map { pdf("Page $it") }

        val assembler = PdfAssembler(documents.first(), pdfMetrics)
        documents.drop(1).forEach(assembler::add)
        val assembledResult = assembler.finish()

        var sequentiallyMerged = documents.first()
        documents.drop(1).forEach { sequentiallyMerged = mergeTheOldWay(sequentiallyMerged, it) }

        assertThat(pageCount(assembledResult)).isEqualTo(pageCount(sequentiallyMerged))
        assertThat(texts(assembledResult)).isEqualTo(texts(sequentiallyMerged))
    }

    @Test
    fun `finish is safe to call even if the assembler is already closed`() {
        val assembler = PdfAssembler(pdf("First"), pdfMetrics)
        val result = assembler.finish()

        assembler.close()

        assertThat(pageCount(result)).isEqualTo(1)
    }


    @Test
    fun `smart mode produces the same content as without it`() {
        val documents = (1..12).map { pdfWithFont("Page $it") }

        val smart = PdfAssembler(documents.first(), pdfMetrics, smartMode = true)
        documents.drop(1).forEach(smart::add)
        val smartResult = smart.finish()

        val plain = PdfAssembler(documents.first(), pdfMetrics, smartMode = false)
        documents.drop(1).forEach(plain::add)
        val plainResult = plain.finish()

        assertThat(pageCount(smartResult)).isEqualTo(pageCount(plainResult))
        assertThat(texts(smartResult)).isEqualTo(texts(plainResult))
    }

    @Test
    fun `smart mode does not copy the same fonts from every source document`() {
        val documents = (1..12).map { pdfWithFont("Page $it") }

        val smart = PdfAssembler(documents.first(), pdfMetrics, smartMode = true)
        documents.drop(1).forEach(smart::add)
        val smartResult = smart.finish()

        val plain = PdfAssembler(documents.first(), pdfMetrics, smartMode = false)
        documents.drop(1).forEach(plain::add)
        val plainResult = plain.finish()

        // Same content, but shared objects: the assembly has to be clearly smaller.
        assertThat(smartResult.size).isLessThan(plainResult.size / 2)
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
    }
}


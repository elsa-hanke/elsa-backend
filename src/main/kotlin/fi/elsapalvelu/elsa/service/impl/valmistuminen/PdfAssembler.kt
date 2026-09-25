package fi.elsapalvelu.elsa.service.impl.valmistuminen

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.utils.PdfMerger
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService
import fi.elsapalvelu.elsa.service.metrics.PdfGenerationMetricsService.Companion.OP_YHDISTA_PDF
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable

/**
 * Assembles the attachment documents of a graduation request into a single PDF document.
 *
 * Every addition used to read and rewrite the whole document assembled so far, which made the
 * work grow quadratically with the number of entries (5092 performance entries came to roughly
 * 27 minutes of merging alone). Here the target document is kept open for the whole assembly,
 * so every addition costs the same regardless of how many pages have already been collected.
 * On the same data that brought the merging down to under two seconds.
 *
 * [smartMode] enables iText's "smart mode". Every performance entry is rendered as its own
 * PDF/A document, so each one carries its own copy of the font subsets, the colour profile and
 * the metadata. Without this mode the merge copies all of them separately (measured at
 * 31 KiB/page); with it, identical objects are shared (measured at 4 KiB/page). On real data
 * that shrank the stored document from 152 MiB to 47 MiB, which considerably shortens saving
 * it to the database - after the assembly, that is the largest phase of the work.
 */
class PdfAssembler(
    firstDocument: ByteArray,
    private val pdfMetrics: PdfGenerationMetricsService,
    smartMode: Boolean = true
) : Closeable {

    private val outputStream = ByteArrayOutputStream()
    private val target = PdfDocument(
        PdfReader(ByteArrayInputStream(firstDocument)),
        if (smartMode) PdfWriter(outputStream, WriterProperties().useSmartMode())
        else PdfWriter(outputStream)
    )
    private val merger = PdfMerger(target)

    private var closed = false

    val pages: Int
        get() = target.numberOfPages

    fun add(pdf: ByteArray) {
        pdfMetrics.trackOperation(OP_YHDISTA_PDF) {
            PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { source ->
                merger.merge(source, 1, source.numberOfPages)
            }
        }
    }

    fun add(pdf: ByteArrayOutputStream) = add(pdf.toByteArray())

    /**
     * Closes the target document and returns the finished PDF as bytes.
     */
    fun finish(): ByteArray {
        close()
        return outputStream.toByteArray()
    }

    override fun close() {
        if (!closed) {
            closed = true
            target.close()
        }
    }
}


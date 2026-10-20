package fi.elsapalvelu.elsa.service

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfAConformanceLevel
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfDictionary
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfObject
import com.itextpdf.kernel.pdf.PdfResources
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfOutputIntent
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.pdfa.PdfADocument
import com.itextpdf.pdfa.checker.PdfA2Checker
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.imageio.ImageIO

/**
 * Creates, validates and normalizes the PDF/A-2b documents used for archiving.
 */
@Service
class PdfA2bService(
    @Value("classpath:sRGB_CS_profile.icm")
    private val colorProfile: Resource
) {

    fun createDocument(output: OutputStream): PdfADocument =
        try {
            colorProfile.inputStream.use { profile ->
                PdfADocument(
                    PdfWriter(output),
                    PdfAConformanceLevel.PDF_A_2B,
                    PdfOutputIntent(
                        "Custom",
                        "",
                        "https://www.color.org",
                        "sRGB IEC61966-2.1",
                        profile
                    )
                )
            }
        } catch (_: Exception) {
            // A missing/broken server-side color profile is not a rejected user document.
            throw PdfA2bInfrastructureException()
        }

    fun isPdfA2b(data: ByteArray?): Boolean {
        if (data == null || data.isEmpty()) {
            return false
        }

        return try {
            // Validate the original in read-only mode: stamping can repair metadata before checking it.
            PdfDocument(PdfReader(ByteArrayInputStream(data))).use { document ->
                if (document.reader.isEncrypted || document.numberOfPages == 0 ||
                    document.reader.pdfAConformanceLevel != PdfAConformanceLevel.PDF_A_2B) {
                    return false
                }
                StrictPdfA2Checker().validate(document)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Every returned byte array has passed full PDF/A validation; callers must preserve its integrity. */
    fun normalize(data: ByteArray?, contentType: String?): ByteArray = normalize(data, contentType, RasterBudget())

    /** Normalize originals separately: one bad attachment must not rasterize its conforming neighbours. */
    internal fun normalizeAndMerge(sources: Sequence<PdfA2bSource>): ByteArray {
        val output = ByteArrayOutputStream()
        val budget = RasterBudget()
        createDocument(output).use { target ->
            sources.forEach { attachment ->
                val normalized = normalize(attachment.data, attachment.contentType, budget)
                PdfDocument(PdfReader(ByteArrayInputStream(normalized))).use { source ->
                    source.copyPagesTo(1, source.numberOfPages, target)
                    // No more copying from this source: release its copied-object cache before the next attachment.
                    target.flushCopiedObjects(source)
                }
            }
            require(target.numberOfPages > 0) { "Archive attachments must contain at least one page" }
        }
        // Validate the closed, assembled document once. Never rasterize the whole merge on failure.
        return output.toByteArray().also { check(isPdfA2b(it)) { "Merged archive document is not PDF/A-2b" } }
    }

    private fun normalize(data: ByteArray?, contentType: String?, budget: RasterBudget): ByteArray {
        require(data != null && data.isNotEmpty()) { "Arkistoitava asiakirja on tyhja" }
        val canonicalType = DocumentContentTypes.canonical(contentType)
        return when (canonicalType) {
            // The PDF fast path must copy before checking: the caller cannot change bytes between validation and return.
            MediaType.APPLICATION_PDF_VALUE -> normalizePdf(data.copyOf(), budget)
            // Image output is newly generated and validated; enforce encoded-size limits before making any extra copies.
            MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE -> imageToPdfA(data, canonicalType, budget)
            else -> throw IllegalArgumentException(
                "Arkistoitavan asiakirjan sisaltotyyppia '$contentType' ei tueta"
            )
        }
    }

    private fun normalizePdf(data: ByteArray, budget: RasterBudget): ByteArray {
        if (isPdfA2b(data)) {
            // normalize already owns this copy; returning it avoids retaining another full PDF array.
            return data
        }

        // Remove permission restrictions from a processing copy; an opening password still fails.
        val prepared = try {
            PdfPreparation.prepare(data)
        } catch (exception: Exception) {
            throw IllegalArgumentException("Arkistoitavaa PDF-tiedostoa ei voida kasitella", exception)
        }
        return try {
            copyPdfToPdfA(prepared).also { check(isPdfA2b(it)) }
        } catch (exception: PdfA2bInfrastructureException) {
            throw exception
        } catch (_: Exception) {
            try {
                rasterizePdfToPdfA(prepared, budget).also { check(isPdfA2b(it)) }
            } catch (exception: PdfA2bInfrastructureException) {
                throw exception
            } catch (exception: Exception) {
                throw IllegalArgumentException("Arkistoitavaa PDF-tiedostoa ei voida kasitella", exception)
            }
        }
    }

    private fun copyPdfToPdfA(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        PdfDocument(PdfReader(ByteArrayInputStream(data))).use { source ->
            require(!source.reader.isEncrypted && source.numberOfPages > 0) {
                "Arkistoitava PDF on salattu tai sivuton"
            }
            createDocument(output).use { target ->
                source.copyPagesTo(1, source.numberOfPages, target)
            }
        }
        return output.toByteArray()
    }

    private fun rasterizePdfToPdfA(data: ByteArray, budget: RasterBudget): ByteArray {
        val output = ByteArrayOutputStream()
        Loader.loadPDF(data).use { source ->
            require(!source.isEncrypted && source.numberOfPages > 0) {
                "Arkistoitava PDF on salattu tai sivuton"
            }
            require(source.numberOfPages <= MAX_RASTER_PAGES) { "PDF contains too many pages to rasterize" }
            source.pages.forEach { page ->
                val width = page.cropBox.width.toDouble() * RASTER_DPI / POINTS_PER_INCH
                val height = page.cropBox.height.toDouble() * RASTER_DPI / POINTS_PER_INCH
                val pixels = kotlin.math.ceil(width) * kotlin.math.ceil(height)
                require(width.isFinite() && height.isFinite() && width > 0 && height > 0 &&
                    pixels <= MAX_PAGE_PIXELS) { "PDF page is too large to rasterize" }
                budget.reserve(pixels)
            }
            val renderer = PDFRenderer(source)
            createDocument(output).use { target ->
                Document(target).use { document ->
                    repeat(source.numberOfPages) { pageIndex ->
                        val rendered = renderer.renderImageWithDPI(
                            pageIndex,
                            RASTER_DPI,
                            ImageType.RGB
                        )
                        try {
                            val imageOutput = ByteArrayOutputStream()
                            check(ImageIO.write(rendered, "png", imageOutput))
                            val pageSize = pageSize(
                                rendered.width * POINTS_PER_INCH / RASTER_DPI,
                                rendered.height * POINTS_PER_INCH / RASTER_DPI
                            )
                            target.addNewPage(pageSize)
                            document.add(
                                Image(ImageDataFactory.create(imageOutput.toByteArray()))
                                    .scaleAbsolute(pageSize.width, pageSize.height)
                                    .setFixedPosition(target.numberOfPages, 0F, 0F)
                            )
                        } finally {
                            rendered.flush()
                        }
                    }
                }
            }
        }
        return output.toByteArray()
    }

    private fun imageToPdfA(data: ByteArray, contentType: String, budget: RasterBudget): ByteArray {
        val output = ByteArrayOutputStream()
        val imageData = ImageDataFactory.create(ArchiveImageNormalizer().normalize(data, contentType))
        budget.reserve(imageData.width.toDouble() * imageData.height)
        val pageSize = pageSize(imageData.width, imageData.height)
        createDocument(output).use { target ->
            Document(target).use { document ->
                target.addNewPage(pageSize)
                document.add(
                    Image(imageData)
                        .scaleToFit(pageSize.width, pageSize.height)
                        .setFixedPosition(1, 0F, 0F)
                )
            }
        }
        return output.toByteArray().also { check(isPdfA2b(it)) }
    }

    private fun pageSize(width: Float, height: Float) = PageSize(
        width.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
        height.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
    )

    /** Shared across original attachments; splitting a PDF must not reset the raster-work limit. */
    private class RasterBudget {
        private var totalPixels = 0.0

        fun reserve(pixels: Double) {
            totalPixels += pixels
            require(totalPixels <= MAX_TOTAL_PIXELS) { "PDF is too large to rasterize" }
        }
    }

    private companion object {
        const val MAX_RASTER_PAGES = 200
        const val MAX_PAGE_PIXELS = 10_000_000.0
        const val MAX_TOTAL_PIXELS = 50_000_000.0
        const val RASTER_DPI = 150F
        const val POINTS_PER_INCH = 72F
        const val MIN_PAGE_SIZE = 3F
        const val MAX_PAGE_SIZE = 14_400F
    }
}

/** Safe marker for server configuration failures; do not retain resource paths or library causes. */
class PdfA2bInfrastructureException : RuntimeException("PDF-A infrastructure unavailable", null)

/** Check existing streams and fonts too; iText's stamping defaults assume a conforming source. */
private class StrictPdfA2Checker : PdfA2Checker(PdfAConformanceLevel.PDF_A_2B) {
    init {
        isFullCheckMode = true
    }

    fun validate(document: PdfDocument) {
        setPdfAOutputIntentColorSpace(document.catalog.pdfObject)
        // checkDocument checks stream operands, but canvas checks need interpreted graphics state.
        // Interpret first so its color/transparency usage is available to the document checks.
        for (pageNumber in 1..document.numberOfPages) {
            val page = document.getPage(pageNumber)
            val stream = if (page.contentStreamCount > 0) page.getContentStream(0) else null
            PdfA2bCanvasValidator(this, stream).processPageContent(page)
        }
        checkDocument(document.catalog)
    }

    override fun checkResources(resources: PdfDictionary?, pdfObject: PdfObject?) {
        resources?.getAsDictionary(PdfName.Font)?.let { fonts ->
            fonts.keySet().forEach { name ->
                checkFont(PdfFontFactory.createFont(fonts.getAsDictionary(name)))
            }
        }
        super.checkResources(resources, pdfObject)
    }

    override fun checkAppearanceStream(appearanceStream: PdfStream) {
        PdfA2bCanvasValidator(this, appearanceStream).processContent(
            appearanceStream.bytes,
            PdfResources(appearanceStream.getAsDictionary(PdfName.Resources) ?: PdfDictionary())
        )
        super.checkAppearanceStream(appearanceStream)
    }
}

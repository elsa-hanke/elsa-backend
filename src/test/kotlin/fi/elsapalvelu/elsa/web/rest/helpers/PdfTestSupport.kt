package fi.elsapalvelu.elsa.web.rest.helpers

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import java.io.ByteArrayOutputStream

/**
 * Helpers for asserting on the *content* of generated PDFs (not just that they open).
 *
 * Used by the valmistumispyynto approval regression tests so that the PDF generation
 * can be optimised without silently dropping sections, reordering pages or losing text.
 */
object PdfTestSupport {

    /** Whitespace-normalised text of every page, in page order. */
    fun pageTexts(data: ByteArray): List<String> =
        Loader.loadPDF(data).use { pdf ->
            val stripper = PDFTextStripper()
            (1..pdf.numberOfPages).map { page ->
                stripper.startPage = page
                stripper.endPage = page
                normalize(stripper.getText(pdf))
            }
        }

    /** Whitespace-normalised text of the whole document. */
    fun text(data: ByteArray): String = pageTexts(data).joinToString(" ")

    fun pageCount(data: ByteArray): Int = Loader.loadPDF(data).use { it.numberOfPages }

    /**
     * Asserts that every fragment occurs in [text] and that the fragments appear in the
     * given order (each one is searched for after the previous one).
     */
    fun assertContainsInOrder(text: String, vararg fragments: String) {
        var from = 0
        fragments.forEach { fragment ->
            val index = text.indexOf(fragment, from)
            assertThat(index)
                .withFailMessage(
                    "Expected PDF text to contain \"%s\" after position %d, but it did not.%nText was: %s",
                    fragment, from, text.take(MAX_FAILURE_TEXT)
                )
                .isGreaterThanOrEqualTo(0)
            from = index + fragment.length
        }
    }


    /**
     * The index (0-based) of every page whose text starts with one of the given section
     * headings, paired with that heading, in page order. Pages that continue a section
     * (do not start with a heading) are left out, so a section that spills over to a second
     * page does not disturb the sequence, but a duplicated or missing section does.
     */
    fun headingSequence(pages: List<String>, headings: List<String>): List<String> =
        pages.mapNotNull { page ->
            headings.firstOrNull { heading ->
                page == heading || page.startsWith("$heading ")
            }
        }

    /** Names of all fonts used on the pages, with the subset prefix ("ABCDEF+") removed. */
    fun fontNames(data: ByteArray): Set<String> =
        Loader.loadPDF(data).use { pdf ->
            pdf.pages.flatMap { page ->
                val resources = page.resources
                resources.fontNames.mapNotNull { name ->
                    resources.getFont(name)?.name?.substringAfter('+')
                }
            }.toSet()
        }

    /**
     * Asserts that the document still declares PDF/A conformance (XMP `pdfaid`) and carries
     * its output intent. These documents are archived, and merging rewrites the object tree.
     */
    fun assertPdfA(data: ByteArray) {
        Loader.loadPDF(data).use { pdf ->
            val catalog = pdf.documentCatalog
            val xmp = catalog.metadata?.exportXMPMetadata()?.use { it.readBytes().toString(Charsets.UTF_8) }
            assertThat(xmp).withFailMessage("The PDF has no XMP metadata").isNotNull()
            assertThat(xmp).withFailMessage("The XMP metadata has no PDF/A identification").contains("pdfaid:part")
            assertThat(catalog.outputIntents).withFailMessage("The PDF has no output intent").isNotEmpty()
        }
    }

    /** Builds a small valid PDF where page N contains [pageTexts]\[N-1\] (ASCII only). */
    fun createPdf(vararg pageTexts: String): ByteArray =
        PDDocument().use { document ->
            pageTexts.forEach { pageText ->
                val page = PDPage()
                document.addPage(page)
                PDPageContentStream(document, page).use { content ->
                    content.beginText()
                    content.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE)
                    content.newLineAtOffset(MARGIN, TOP)
                    content.showText(pageText)
                    content.endText()
                }
            }
            ByteArrayOutputStream().also { document.save(it) }.toByteArray()
        }

    private fun normalize(text: String) = text.replace(Regex("\\s+"), " ").trim()

    private const val FONT_SIZE = 14f
    private const val MARGIN = 72f
    private const val TOP = 700f
    private const val MAX_FAILURE_TEXT = 2000
}

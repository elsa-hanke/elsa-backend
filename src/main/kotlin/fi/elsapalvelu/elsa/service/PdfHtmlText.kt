package fi.elsapalvelu.elsa.service

import com.itextpdf.styledxmlparser.jsoup.Jsoup

/** Uses the same HTML parser as pdfHTML; entities are decoded exactly once. */
object PdfHtmlText {
    fun text(html: String?): String? = html?.let {
        val document = Jsoup.parse(it)
        document.select("script, style").remove()
        document.body().text()
    }

    /** Sanitize decoded text nodes as well as literal characters before conversion. */
    fun sanitize(html: String): String {
        val document = Jsoup.parse(PdfTextSanitizer.sanitize(html))
        document.outputSettings().prettyPrint(false)
        document.getAllElements().forEach { element ->
            element.textNodes().forEach { node ->
                node.text(PdfTextSanitizer.sanitize(node.wholeText))
            }
        }
        return document.outerHtml()
    }
}

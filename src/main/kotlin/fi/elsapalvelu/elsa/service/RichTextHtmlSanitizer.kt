package fi.elsapalvelu.elsa.service

import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.AttributePolicy
import org.springframework.web.util.HtmlUtils

/** HTML authored in the restricted rich-text editor, never arbitrary active content or CSS. */
object RichTextHtmlSanitizer {
    private val positiveSpan = AttributePolicy { _, _, value ->
        value.toIntOrNull()?.takeIf { it in 1..1000 }?.toString()
    }
    private val policy = HtmlPolicyBuilder()
        .allowElements(
            "p", "br", "h2", "h3", "h4", "strong", "b", "em", "i", "u", "s",
            "ul", "ol", "li", "blockquote", "a", "table", "caption", "colgroup", "col",
            "thead", "tbody", "tfoot", "tr", "th", "td"
        )
        .allowWithoutAttributes("a")
        .allowUrlProtocols("http", "https", "mailto")
        .allowAttributes("href", "title").onElements("a")
        .allowAttributes("colspan", "rowspan").matching(positiveSpan).onElements("td", "th")
        .allowAttributes("span").matching(positiveSpan).onElements("col", "colgroup")
        .allowAttributes("scope").matching(true, "row", "col", "rowgroup", "colgroup").onElements("th")
        .allowAttributes("start").matching { _, _, value -> value.toIntOrNull()?.toString() }.onElements("ol")
        .toFactory()

    private val textOnlyPolicy = HtmlPolicyBuilder().toFactory()

    fun hasText(html: String?): Boolean =
        HtmlUtils.htmlUnescape(textOnlyPolicy.sanitize(html.orEmpty()))
            .any { !it.isWhitespace() && !Character.isSpaceChar(it) }

    fun sanitize(html: String?): String? = html?.let(policy::sanitize)
}

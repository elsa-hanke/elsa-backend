package fi.elsapalvelu.elsa.service.seuranta

import org.owasp.html.HtmlPolicyBuilder

/** HTML authored in the announcement editor, never arbitrary active content or CSS. */
object IlmoitusHtmlSanitizer {
    private val policy = HtmlPolicyBuilder()
        .allowElements(
            "p", "br", "h2", "h3", "h4", "strong", "b", "em", "i", "u", "s",
            "ul", "ol", "li", "blockquote", "a", "table", "caption", "colgroup", "col",
            "thead", "tbody", "tfoot", "tr", "th", "td"
        )
        .allowWithoutAttributes("a")
        .allowUrlProtocols("http", "https", "mailto")
        .allowAttributes("href", "title").onElements("a")
        .allowAttributes("colspan", "rowspan", "span")
        .matching { _, _, value -> value.toIntOrNull()?.takeIf { it in 1..1000 }?.toString() }
        .onElements("td", "th", "col", "colgroup")
        .allowAttributes("scope").matching(true, "row", "col", "rowgroup", "colgroup").onElements("th")
        .allowAttributes("start").matching { _, _, value -> value.toIntOrNull()?.toString() }.onElements("ol")
        .toFactory()

    fun sanitize(html: String?): String? = html?.let(policy::sanitize)
}

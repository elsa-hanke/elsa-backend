package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFailsWith

class PdfHtmlTextTest {
    private val validator = PdfTextTestSupport.fieldValidator()

    @ParameterizedTest
    @ValueSource(strings = ["💗", "&#x1F497;", "&#X1f497;", "&#128151;"])
    fun `rejects literal and encoded supplementary characters with field details`(character: String) {
        val exception = assertFailsWith<UnsupportedPdfCharactersException> {
            validator.validate(
                fields = emptyList(),
                htmlFields = listOf("yhteenveto" to "<p>Hyvä <strong>$character</strong></p>"),
                pdfSource = "koejaksonvastuuhenkilonarvio",
                sourceId = 42L
            )
        }
        assertThat(exception.field).isEqualTo("yhteenveto")
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        assertThat(exception.sourceId).isEqualTo(42L)
    }

    @Test
    fun `decodes named entities before validating font support`() {
        val exception = assertFailsWith<UnsupportedPdfCharactersException> {
            validator.validate(emptyList(), htmlFields = listOf("yhteenveto" to "<p>&check;</p>"))
        }
        assertThat(exception.unsupportedCharacters).containsExactly("✓ (U+2713)")
    }

    @Test
    fun `valid HTML formatting entities and Finnish text remain allowed`() {
        validator.validate(emptyList(), htmlFields = listOf(
            "yhteenveto" to "<p>Hyvä lääkäri &amp; kouluttaja&nbsp;– <strong>Jyväskylä</strong></p><ul><li>Tavoite</li></ul>"
        ))
        assertThat(PdfHtmlText.text("<p>Hyvä &amp; lääkäri</p>")).isEqualTo("Hyvä & lääkäri")
    }

    @Test
    fun `ignores non-text metadata scripts styles and attributes`() {
        val html = "<html><head><title>💗</title><style>/* 💗 */</style></head>" +
            "<body><p data-note='💗'>Hyvä</p><script>💗</script></body></html>"
        validator.validate(emptyList(), htmlFields = listOf("yhteenveto" to html))
        assertThat(PdfHtmlText.text(html)).isEqualTo("Hyvä")
    }

    @Test
    fun `does not decode plain text or double-decode escaped HTML entities`() {
        validator.validate(listOf("tavallinen-kentta" to "&#x1F497;"))
        validator.validate(emptyList(), htmlFields = listOf("yhteenveto" to "<p>&amp;#x1F497;</p>"))
        assertThat(PdfHtmlText.text("<p>&amp;#x1F497;</p>")).isEqualTo("&#x1F497;")
        assertThat(PdfHtmlText.text("<p>&lt;strong&gt;Ohje&lt;/strong&gt;</p>"))
            .isEqualTo("<strong>Ohje</strong>")
    }

    @Test
    fun `sanitizes encoded Wingdings text consistently with literal symbols`() {
        val html = "<p>Luettelo &#xF0B7; kohta &#xF0A7; alakohta &#xE001;</p>"
        val sanitized = PdfHtmlText.sanitize(html)
        assertThat(PdfHtmlText.text(sanitized)).isEqualTo("Luettelo • kohta ◦ alakohta")
        validator.validate(emptyList(), htmlFields = listOf("yhteenveto" to sanitized))
    }

    @Test
    fun `sanitization preserves escaped entity text and whitespace`() {
        val html = "<p>&amp;#x1F497;\n  Toinen rivi</p>"
        val sanitized = PdfHtmlText.sanitize(html)
        assertThat(sanitized).contains("&amp;#x1F497;\n  Toinen rivi")
        assertThat(PdfHtmlText.text(sanitized)).isEqualTo("&#x1F497; Toinen rivi")
    }

    @Test
    fun `empty and unfinished HTML fragments are handled`() {
        validator.validate(emptyList(), htmlFields = listOf("yhteenveto" to null, "yhteenveto" to ""))
        assertThat(PdfHtmlText.text(null)).isNull()
        assertThat(PdfHtmlText.text("<p>Hyvä <strong>lääkäri")).isEqualTo("Hyvä lääkäri")
    }
}

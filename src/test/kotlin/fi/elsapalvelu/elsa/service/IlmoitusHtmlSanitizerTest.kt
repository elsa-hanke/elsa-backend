package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.seuranta.IlmoitusHtmlSanitizer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IlmoitusHtmlSanitizerTest {
    @ParameterizedTest
    @ValueSource(strings = [
        "<p onclick='alert(1)'>safe</p>",
        "<a href='javascript:alert(1)'>safe</a>",
        "<a href='jav&#x61;script:alert(1)'>safe</a>",
        "<a href='data:text/html,evil'>safe</a>",
        "<svg onload='alert(1)'></svg><p>safe</p>",
        "<iframe src='https://evil.example'>evil</iframe><p>safe</p>",
        "<script>alert(1)</script><p>safe</p>",
        "<p style='background:url(https://evil.example);position:fixed'>safe</p>",
        "<form><input autofocus onfocus='alert(1)'></form><p>safe</p>"
    ])
    fun `removes active content and unsafe attributes`(html: String) {
        val result = IlmoitusHtmlSanitizer.sanitize(html)!!
        assertThat(result).contains("safe").doesNotContain(
            "onclick", "onload", "onfocus", "javascript:", "data:", "<svg", "<iframe", "<script",
            "<input", "<form", "style=", "evil", "alert(1)"
        )
    }

    @Test
    fun `preserves editor formatting tables links and Finnish entities without double escaping`() {
        val html = "<h2>Hyvää &amp; tärkeää</h2><p><strong>Uutinen</strong><br /><em>teksti</em></p>" +
            "<ul><li>yksi</li></ul><ol start=\"2\"><li>kaksi</li></ol>" +
            "<table><tbody><tr><th scope=\"col\">Otsikko</th><td colspan=\"2\">Solu</td></tr></tbody></table>" +
            "<a href=\"https://example.org\" title=\"Linkki\">https</a>" +
            "<a href=\"mailto:test@example.org\">mail</a><a href=\"/ohje\">relative</a>"
        val sanitized = IlmoitusHtmlSanitizer.sanitize(html)
        assertThat(sanitized).isEqualTo(html.replace("test@example.org", "test&#64;example.org"))
        assertThat(IlmoitusHtmlSanitizer.sanitize(sanitized)).isEqualTo(sanitized)
    }

    @Test
    fun `drops CSS target and non editor attributes`() {
        assertThat(IlmoitusHtmlSanitizer.sanitize("<p id='x' style='color:red'>Hi</p><a href='/ohje' target='_blank'>link</a>"))
            .isEqualTo("<p>Hi</p><a href=\"/ohje\">link</a>")
        assertThat(IlmoitusHtmlSanitizer.sanitize(null)).isNull()
    }
}

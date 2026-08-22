package fi.elsapalvelu.elsa.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PdfTextSanitizerTest {

    @Test
    fun `replaces the two historically supported Wingdings bullets`() {
        assertThat(PdfTextSanitizer.sanitize("\uF0B7 ensimmäinen, \uF0A7 toinen"))
            .isEqualTo("• ensimmäinen, ◦ toinen")
    }

    @Test
    fun `removes other private-use characters`() {
        assertThat(PdfTextSanitizer.sanitize("ennen \uE123 jälkeen"))
            .isEqualTo("ennen  jälkeen")
    }

    @Test
    fun `does not replace a check mark or emoji`() {
        assertThat(PdfTextSanitizer.sanitize("valmis ✓ 😀"))
            .isEqualTo("valmis ✓ 😀")
    }
}

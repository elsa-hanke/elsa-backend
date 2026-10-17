package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate
import kotlin.test.assertFailsWith

class PdfTextFieldValidatorTest {

    private val validator = PdfTextFieldValidator(
        PdfTextValidator(
            ClassPathResource("fonts/LiberationSerif-Bold.ttf"),
            ClassPathResource("fonts/LiberationSerif-Regular.ttf"),
            ClassPathResource("fonts/NotoSans-Italic.ttf"),
            ClassPathResource("fonts/NotoSans-Regular.ttf")
        )
    )

    @Test
    fun `reports field and source context for an unsupported character`() {
        val sourceDate = LocalDate.of(2025, 5, 15)

        val exception = assertFailsWith<UnsupportedPdfCharactersException> {
            validator.validate(
                fields = listOf(
                    "ensimmainen-kentta" to "Tuettu teksti",
                    "toinen-kentta" to "Ei tuettu ✓"
                ),
                pdfSource = "paivakirjamerkinta",
                sourceId = 42L,
                sourceDate = sourceDate
            )
        }

        assertThat(exception.field).isEqualTo("toinen-kentta")
        assertThat(exception.unsupportedCharacters).containsExactly("✓ (U+2713)")
        assertThat(exception.pdfSource).isEqualTo("paivakirjamerkinta")
        assertThat(exception.sourceId).isEqualTo(42L)
        assertThat(exception.sourceDate).isEqualTo(sourceDate)
    }

    @Test
    fun `sanitizes copied Wingdings symbols before validating`() {
        validator.validate(
            fields = listOf("kentta" to "Luettelo \uF0B7 kohta \uF0A7 alakohta")
        )
    }

    @Test
    fun `accepts supported invisible characters in plain text and encoded HTML`() {
        validator.validate(
            fields = listOf("kentta" to "Hyvä\u200B lääkäri\u00AD –\u00A0Jyväskylä\u2028Toinen rivi"),
            htmlFields = listOf("yhteenveto" to "<p>Hyvä&#x200B; lääkäri&#xAD; –&nbsp;Jyväskylä&#x2028;Toinen rivi</p>")
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["\uFE0E", "\uFE0F"])
    fun `reports an unsupported variation selector explicitly in plain text and HTML`(selector: String) {
        val codePoint = "U+${selector[0].code.toString(16).uppercase()}"
        val entity = "&#${selector[0].code};"
        listOf(false, true).forEach { html ->
            val exception = assertFailsWith<UnsupportedPdfCharactersException> {
                validator.validate(
                    fields = if (html) emptyList() else listOf("kentta" to "→$selector"),
                    htmlFields = if (html) listOf("kentta" to "<p>→$entity</p>") else emptyList()
                )
            }
            assertThat(exception.field).isEqualTo("kentta")
            assertThat(exception.unsupportedCharacters).containsExactly("$selector ($codePoint)")
        }
    }
}

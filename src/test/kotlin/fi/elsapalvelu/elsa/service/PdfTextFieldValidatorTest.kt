package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
}

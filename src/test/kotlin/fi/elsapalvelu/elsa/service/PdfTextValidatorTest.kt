package fi.elsapalvelu.elsa.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class PdfTextValidatorTest {

    private val validator = PdfTextValidator(
        ClassPathResource("fonts/LiberationSerif-Bold.ttf"),
        ClassPathResource("fonts/LiberationSerif-Regular.ttf"),
        ClassPathResource("fonts/NotoSans-Italic.ttf"),
        ClassPathResource("fonts/NotoSans-Regular.ttf")
    )

    @Test
    fun `accepts Finnish text, supported symbols and line breaks`() {
        val text = "Ääkköset, nuoli → ja luettelomerkki •\nToinen rivi"

        assertThat(validator.findUnsupportedCharacters(text)).isEmpty()
    }

    @Test
    fun `rejects a check mark that is missing from the PDF fonts`() {
        assertThat(validator.findUnsupportedCharacters("Valmis ✓"))
            .containsExactly(UnsupportedPdfCharacter("✓", "U+2713"))
    }

    @Test
    fun `rejects emoji and handles supplementary Unicode code points as one character`() {
        assertThat(validator.findUnsupportedCharacters("Hyvä 😀"))
            .containsExactly(UnsupportedPdfCharacter("😀", "U+1F600"))
    }

    @Test
    fun `rejects copied private-use symbols and reports each character once`() {
        assertThat(validator.findUnsupportedCharacters("\uF0B7 \uF0B7 \uF0A7"))
            .containsExactly(
                UnsupportedPdfCharacter("\uF0B7", "U+F0B7"),
                UnsupportedPdfCharacter("\uF0A7", "U+F0A7")
            )
    }

    @Test
    fun `accepts null and empty text`() {
        assertThat(validator.findUnsupportedCharacters(null)).isEmpty()
        assertThat(validator.findUnsupportedCharacters("")).isEmpty()
    }
}

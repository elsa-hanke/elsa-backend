package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.service.PdfTextTestSupport
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFailsWith

class VastuuhenkilonArvioPdfTextValidatorTest {
    private val validator = VastuuhenkilonArvioPdfTextValidator(PdfTextTestSupport.fieldValidator())

    @ParameterizedTest
    @CsvSource(
        "create,erikoistuvanSahkoposti,sahkoposti", "create,erikoistuvanPuhelinnumero,puhelinnumero",
        "create,perusteluHylkaamiselle,perustelu-hylkaamiselle", "create,lisatiedotVirkailijalta,lisatiedot-vastuuhenkilolle",
        "create,virkailijanKorjausehdotus,korjausehdotus", "create,vastuuhenkilonKorjausehdotus,korjausehdotus",
        "create,virkailijanYhteenveto,virkailijan-valmistumisen-yhteenveto",
        "trainee,erikoistuvanSahkoposti,sahkoposti", "trainee,erikoistuvanPuhelinnumero,puhelinnumero",
        "officer,lisatiedotVirkailijalta,lisatiedot-vastuuhenkilolle", "officer,virkailijanKorjausehdotus,korjausehdotus",
        "officer,virkailijanYhteenveto,virkailijan-valmistumisen-yhteenveto",
        "responsible,vastuuhenkilonSahkoposti,sahkoposti", "responsible,vastuuhenkilonPuhelinnumero,puhelinnumero",
        "responsible,vastuuhenkilonKorjausehdotus,korjausehdotus", "responsible,perusteluHylkaamiselle,perustelu-hylkaamiselle"
    )
    fun `reports unsupported writable fields for each operation`(role: String, property: String, field: String) {
        val dto = KoejaksonVastuuhenkilonArvioDTO(id = 42L, koejaksoHyvaksytty = false)
        val setter = "set" + property.replaceFirstChar { it.uppercaseChar() }
        dto.javaClass.getMethod(setter, String::class.java).invoke(dto, "💗")

        val exception = assertFailsWith<UnsupportedPdfCharactersException> {
            when (role) {
                "create" -> validator.validateLuonti(dto, emptyList())
                "trainee" -> validator.validateErikoistujanKentat(dto, emptyList())
                "officer" -> validator.validateVirkailijanKentat(dto)
                else -> validator.validateVastuuhenkilonKentat(dto)
            }
        }
        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.sourceId).isEqualTo(42L)
        assertThat(exception.pdfSource).isEqualTo("koejaksonvastuuhenkilonarvio")
    }

    @ParameterizedTest
    @ValueSource(strings = ["create", "trainee"])
    fun `attachment names are checked before persistence`(role: String) {
        val dto = KoejaksonVastuuhenkilonArvioDTO()
        val exception = assertFailsWith<UnsupportedPdfCharactersException> {
            if (role == "create") validator.validateLuonti(dto, listOf("todistus💗.pdf"))
            else validator.validateErikoistujanKentat(dto, listOf("todistus💗.pdf"))
        }
        assertThat(exception.field).isEqualTo("liitetiedoston-nimi")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Korjaa päivämäärä"])
    fun `encoded heart in officer summary is rejected on approval and return`(correction: String) {
        val dto = KoejaksonVastuuhenkilonArvioDTO(
            virkailijanYhteenveto = "<p>&#x1F497;</p>", virkailijanKorjausehdotus = correction
        )
        val exception = assertFailsWith<UnsupportedPdfCharactersException> { validator.validateVirkailijanKentat(dto) }
        assertThat(exception.field).isEqualTo("virkailijan-valmistumisen-yhteenveto")
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
    }

    @Test
    fun `ignored values belonging to another role do not block updates`() {
        validator.validateErikoistujanKentat(KoejaksonVastuuhenkilonArvioDTO(
            virkailijanYhteenveto = "💗", perusteluHylkaamiselle = "💗"
        ), emptyList())
        validator.validateVirkailijanKentat(KoejaksonVastuuhenkilonArvioDTO(
            virkailijanKorjausehdotus = "Korjaa päivämäärä", lisatiedotVirkailijalta = "💗", perusteluHylkaamiselle = "💗"
        ))
        validator.validateVastuuhenkilonKentat(KoejaksonVastuuhenkilonArvioDTO(
            koejaksoHyvaksytty = true, perusteluHylkaamiselle = "💗", virkailijanYhteenveto = "💗"
        ))
    }

    @Test
    fun `valid Finnish summary formatting and literal encoded plain text are allowed`() {
        val dto = KoejaksonVastuuhenkilonArvioDTO(
            erikoistuvanSahkoposti = "test@example.fi", erikoistuvanPuhelinnumero = "+358501234567",
            virkailijanYhteenveto = "<p><strong>Hyvä lääkäri</strong> &amp; pätevä kouluttaja</p>",
            lisatiedotVirkailijalta = "Kirjaimellinen teksti &#x1F497;", koejaksoHyvaksytty = false,
            perusteluHylkaamiselle = "Tarvitaan lisää harjoittelua"
        )
        validator.validateLuonti(dto, listOf("Työtodistus.pdf"))
        validator.validateErikoistujanKentat(dto, emptyList())
        validator.validateVirkailijanKentat(dto)
        validator.validateVastuuhenkilonKentat(dto)
    }
}

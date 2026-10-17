package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.service.PdfTextTestSupport
import fi.elsapalvelu.elsa.service.dto.koejakso.*
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertFailsWith

class KoulutussopimusPdfTextValidatorTest {
    private val validator = KoulutussopimusPdfTextValidator(PdfTextTestSupport.fieldValidator())

    @ParameterizedTest
    @CsvSource(
        "trainee,email,sahkoposti", "trainee,phone,puhelinnumero", "trainee,place,koulutuspaikan-nimi",
        "trainee,workplace,toimipaikka", "trainee,address,lahiosoite", "trainee,city,postitoimipaikka",
        "trainer,workplace,toimipaikka", "trainer,address,lahiosoite", "trainer,city,postitoimipaikka",
        "trainer,title,nimike", "trainer,email,sahkoposti", "trainer,phone,puhelinnumero", "trainer,correction,korjausehdotus",
        "responsible,email,sahkoposti", "responsible,phone,puhelinnumero", "responsible,correction,korjausehdotus"
    )
    @Suppress("CyclomaticComplexMethod")
    fun `reports each writable unsupported field for its role`(role: String, property: String, field: String) {
        val trainer = KoulutussopimuksenKouluttajaDTO(id = 2L, kayttajaUserId = "editor")
        val dto = KoejaksonKoulutussopimusDTO(
            id = 42L, kouluttajat = mutableSetOf(trainer),
            koulutuspaikat = mutableSetOf(KoulutussopimuksenKoulutuspaikkaDTO()),
            vastuuhenkilo = KoulutussopimuksenVastuuhenkiloDTO()
        )
        when (property) {
            "email" -> when (role) {
                "trainee" -> dto.erikoistuvanSahkoposti = "💗"
                "trainer" -> trainer.sahkoposti = "💗"
                else -> dto.vastuuhenkilo?.sahkoposti = "💗"
            }
            "phone" -> when (role) {
                "trainee" -> dto.erikoistuvanPuhelinnumero = "💗"
                "trainer" -> trainer.puhelin = "💗"
                else -> dto.vastuuhenkilo?.puhelin = "💗"
            }
            "place" -> dto.koulutuspaikat?.first()?.nimi = "💗"
            "workplace" -> trainer.toimipaikka = "💗"
            "address" -> trainer.lahiosoite = "💗"
            "city" -> trainer.postitoimipaikka = "💗"
            "title" -> trainer.nimike = "💗"
            "correction" -> dto.korjausehdotus = "💗"
        }
        val exception = assertFailsWith<UnsupportedPdfCharactersException> {
            when (role) {
                "trainee" -> validator.validateErikoistujanKentat(dto)
                "trainer" -> validator.validateKouluttajanKentat(dto, 2L, "editor")
                else -> validator.validateVastuuhenkilonKentat(dto)
            }
        }
        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        assertThat(exception.sourceId).isEqualTo(42L)
    }

    @Test
    fun `trainer validates only own addresses and contact fields`() {
        val dto = KoejaksonKoulutussopimusDTO(
            erikoistuvanSahkoposti = "💗",
            koulutuspaikat = mutableSetOf(KoulutussopimuksenKoulutuspaikkaDTO(nimi = "💗")),
            kouluttajat = mutableSetOf(
                KoulutussopimuksenKouluttajaDTO(id = 1L, kayttajaUserId = "other", nimike = "💗", toimipaikka = "💗"),
                KoulutussopimuksenKouluttajaDTO(id = 2L, kayttajaUserId = "editor", nimike = "Lääkäri", toimipaikka = "Jyväskylä")
            )
        )
        validator.validateKouluttajanKentat(dto, 2L, "editor")
        validator.validateVastuuhenkilonKentat(dto)
    }

    @Test
    fun `trainee ignores contact values that are not persisted from trainee input`() {
        val dto = KoejaksonKoulutussopimusDTO(kouluttajat = mutableSetOf(
            KoulutussopimuksenKouluttajaDTO(nimike = "💗", sahkoposti = "💗", puhelin = "💗")
        ), vastuuhenkilo = KoulutussopimuksenVastuuhenkiloDTO(sahkoposti = "💗"))
        validator.validateErikoistujanKentat(dto)
    }

    @Test
    fun `valid Finnish addresses and normal contact details remain allowed`() {
        val dto = KoejaksonKoulutussopimusDTO(
            erikoistuvanSahkoposti = "test@example.fi", erikoistuvanPuhelinnumero = "+358501234567",
            koulutuspaikat = mutableSetOf(KoulutussopimuksenKoulutuspaikkaDTO(nimi = "Jyväskylän terveyskeskus")),
            kouluttajat = mutableSetOf(KoulutussopimuksenKouluttajaDTO(
                id = 2L, kayttajaUserId = "editor", nimike = "Lääkäri", lahiosoite = "Lääkärinkatu 1", postitoimipaikka = "Jyväskylä"
            ))
        )
        validator.validateErikoistujanKentat(dto)
        validator.validateKouluttajanKentat(dto, 2L, "editor")
        validator.validateVastuuhenkilonKentat(dto)
    }
}

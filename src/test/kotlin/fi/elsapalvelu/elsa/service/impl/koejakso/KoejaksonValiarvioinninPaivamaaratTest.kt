package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonValiarviointi
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonValiarviointiDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonValiarviointiMapper
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class KoejaksonValiarvioinninPaivamaaratTest {
    private val valiarvioinnit = mock<KoejaksonValiarviointiRepository>()
    private val valiarviointiMapper = mock<KoejaksonValiarviointiMapper>()
    private val kayttajat = mock<KayttajaRepository>()
    private val ilmoitukset = mock<MailService>()
    private val lahikouluttaja = Kayttaja(id = 2L, user = User(id = "lahikouluttaja"))
    private val lahiesimies = Kayttaja(id = 3L, user = User(id = "lahiesimies"))
    private val erikoistuja = Kayttaja(id = 4L, user = User(id = "erikoistuja"))
    private val aiempiPaiva: LocalDate = LocalDate.of(2019, 1, 1)
    private val lomake = KoejaksonValiarviointiDTO(id = 1L)
    private val muutokset = KoejaksonValiarviointi()

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttajan uusi kuittaus kayttaa kellon paivaa ja poistaa korjausehdotuksen`(aika: Paivamaaratapaus) {
        val valiarviointi = valmisteleValiarviointi().apply {
            lahikouluttajanKuittausaika = aiempiPaiva
            korjausehdotus = "Tarkenna tavoitteet"
        }
        muutokset.vahvuudet = "Potilaan kohtaaminen"

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(valiarviointi.lahikouluttajanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(valiarviointi.lahikouluttajaHyvaksynyt).isTrue()
        assertThat(valiarviointi.lahiesimiehenKuittausaika).isNull()
        assertThat(valiarviointi.korjausehdotus).isNull()
        assertThat(valiarviointi.vahvuudet).isEqualTo("Potilaan kohtaaminen")
        verify(valiarvioinnit).save(valiarviointi)
    }

    @ParameterizedTest
    @MethodSource("hyvaksymistapaukset")
    fun `lahiesimiehen hyvaksynta kayttaa kellon paivaa ja sailyttaa aiemmat kuittaukset`(tapaus: Hyvaksymistapaus) {
        val valiarviointi = valmisteleValiarviointi().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = tapaus.korjausehdotus

        luoPalvelu(tapaus.aika).update(lomake, "lahiesimies")

        assertThat(valiarviointi.lahiesimiehenKuittausaika).isEqualTo(tapaus.aika.odotettu)
        assertThat(valiarviointi.lahiesimiesHyvaksynyt).isTrue()
        assertThat(valiarviointi.lahikouluttajanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(valiarviointi.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        verify(valiarvioinnit).save(valiarviointi)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahiesimiehen palautus poistaa kouluttajan kuittauksen asettamatta uutta paivaa`(aika: Paivamaaratapaus) {
        val valiarviointi = valmisteleValiarviointi().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = "Tarkenna tavoitteet"

        luoPalvelu(aika).update(lomake, "lahiesimies")

        assertThat(valiarviointi.lahikouluttajanKuittausaika).isNull()
        assertThat(valiarviointi.lahikouluttajaHyvaksynyt).isFalse()
        assertThat(valiarviointi.lahiesimiehenKuittausaika).isNull()
        assertThat(valiarviointi.lahiesimiesHyvaksynyt).isFalse()
        assertThat(valiarviointi.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(valiarviointi.korjausehdotus).isEqualTo("Tarkenna tavoitteet")
        verify(valiarvioinnit).save(valiarviointi)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `sama lahikouluttaja ja lahiesimies kuittaa molemmat roolit kellon paivalla`(aika: Paivamaaratapaus) {
        val valiarviointi = valmisteleValiarviointi().apply { lahiesimies = lahikouluttaja }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(valiarviointi.lahikouluttajanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(valiarviointi.lahiesimiehenKuittausaika).isEqualTo(aika.odotettu)
        assertThat(valiarviointi.lahikouluttajaHyvaksynyt).isTrue()
        assertThat(valiarviointi.lahiesimiesHyvaksynyt).isTrue()
        verify(valiarvioinnit, times(2)).save(valiarviointi)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttaja ei muuta valmiin arvioinnin kuittauspaivia`(aika: Paivamaaratapaus) {
        val valiarviointi = valmisteleValiarviointi().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
            lahiesimiesHyvaksynyt = true
            lahiesimiehenKuittausaika = aiempiPaiva
        }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(valiarviointi.lahikouluttajanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(valiarviointi.lahiesimiehenKuittausaika).isEqualTo(aiempiPaiva)
        verify(valiarvioinnit, never()).save(any<KoejaksonValiarviointi>())
        verifyNoInteractions(ilmoitukset)
    }

    private fun valmisteleValiarviointi(): KoejaksonValiarviointi {
        val valiarviointi = KoejaksonValiarviointi(
            id = 1L,
            lahikouluttaja = lahikouluttaja,
            lahiesimies = lahiesimies,
            opintooikeus = Opintooikeus(erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = erikoistuja)),
            erikoistuvanKuittausaika = aiempiPaiva
        )
        whenever(valiarvioinnit.findById(1L)).thenReturn(Optional.of(valiarviointi))
        whenever(valiarvioinnit.save(any<KoejaksonValiarviointi>())).thenAnswer { it.arguments[0] }
        whenever(valiarviointiMapper.toEntity(lomake)).thenReturn(muutokset)
        whenever(valiarviointiMapper.toDto(valiarviointi)).thenReturn(lomake)
        listOf(lahikouluttaja, lahiesimies, erikoistuja).forEach { kayttaja ->
            whenever(kayttajat.findById(kayttaja.id!!)).thenReturn(Optional.of(kayttaja))
        }
        return valiarviointi
    }

    private fun luoPalvelu(aika: Paivamaaratapaus) = KoejaksonValiarviointiServiceImpl(
        koejaksonValiarviointiRepository = valiarvioinnit,
        koejaksonValiarviointiMapper = valiarviointiMapper,
        mailService = ilmoitukset,
        kayttajaRepository = kayttajat,
        opintooikeusRepository = mock(),
        koejaksonAloituskeskusteluRepository = mock(),
        kouluttajavaltuutusService = mock(),
        opintooikeusService = mock(),
        pdfTextFieldValidator = mock(),
        clock = aika.kello()
    )

    data class Paivamaaratapaus(val hetki: String, val aikavyohyke: String, val odotettu: LocalDate) {
        fun kello(): Clock = Clock.fixed(Instant.parse(hetki), ZoneId.of(aikavyohyke))
    }

    data class Hyvaksymistapaus(val aika: Paivamaaratapaus, val korjausehdotus: String?)

    companion object {
        @JvmStatic
        fun paivamaaratapaukset(): List<Paivamaaratapaus> = listOf(
            Paivamaaratapaus("2020-02-29T22:30:00Z", "UTC", LocalDate.of(2020, 2, 29)),
            Paivamaaratapaus("2020-02-29T22:30:00Z", "Europe/Helsinki", LocalDate.of(2020, 3, 1)),
            Paivamaaratapaus("2020-12-31T22:30:00Z", "Europe/Helsinki", LocalDate.of(2021, 1, 1))
        )

        @JvmStatic
        fun hyvaksymistapaukset(): List<Hyvaksymistapaus> = paivamaaratapaukset().flatMap { aika ->
            listOf(null, "", "  ").map { korjausehdotus -> Hyvaksymistapaus(aika, korjausehdotus) }
        }
    }
}

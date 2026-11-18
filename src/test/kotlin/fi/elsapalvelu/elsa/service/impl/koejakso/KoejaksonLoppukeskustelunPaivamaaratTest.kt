package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonLoppukeskusteluRepository
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonLoppukeskusteluDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonLoppukeskusteluMapper
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

class KoejaksonLoppukeskustelunPaivamaaratTest {
    private val loppukeskustelut = mock<KoejaksonLoppukeskusteluRepository>()
    private val loppukeskusteluMapper = mock<KoejaksonLoppukeskusteluMapper>()
    private val kayttajat = mock<KayttajaRepository>()
    private val ilmoitukset = mock<MailService>()
    private val lahikouluttaja = Kayttaja(id = 2L, user = User(id = "lahikouluttaja"))
    private val lahiesimies = Kayttaja(id = 3L, user = User(id = "lahiesimies"))
    private val erikoistuja = Kayttaja(id = 4L, user = User(id = "erikoistuja"))
    private val aiempiPaiva: LocalDate = LocalDate.of(2019, 1, 1)
    private val lomake = KoejaksonLoppukeskusteluDTO(id = 1L)
    private val muutokset = KoejaksonLoppukeskustelu()

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttajan uusi kuittaus kayttaa kellon paivaa ja poistaa korjausehdotuksen`(aika: Paivamaaratapaus) {
        val loppukeskustelu = valmisteleLoppukeskustelu().apply {
            lahikouluttajanKuittausaika = aiempiPaiva
            korjausehdotus = "Tarkenna jatkotoimenpiteet"
        }
        muutokset.esitetaanKoejaksonHyvaksymista = false
        muutokset.jatkotoimenpiteet = "Lisää ohjattua työskentelyä"

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(loppukeskustelu.lahikouluttajanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(loppukeskustelu.lahikouluttajaHyvaksynyt).isTrue()
        assertThat(loppukeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(loppukeskustelu.korjausehdotus).isNull()
        assertThat(loppukeskustelu.esitetaanKoejaksonHyvaksymista).isFalse()
        assertThat(loppukeskustelu.jatkotoimenpiteet).isEqualTo("Lisää ohjattua työskentelyä")
        assertThat(loppukeskustelu.koejaksonPaattymispaiva).isEqualTo(aiempiPaiva)
        verify(loppukeskustelut).save(loppukeskustelu)
    }

    @ParameterizedTest
    @MethodSource("hyvaksymistapaukset")
    fun `lahiesimiehen hyvaksynta kayttaa kellon paivaa ja sailyttaa aiemmat kuittaukset`(tapaus: Hyvaksymistapaus) {
        val loppukeskustelu = valmisteleLoppukeskustelu().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = tapaus.korjausehdotus

        luoPalvelu(tapaus.aika).update(lomake, "lahiesimies")

        assertThat(loppukeskustelu.lahiesimiehenKuittausaika).isEqualTo(tapaus.aika.odotettu)
        assertThat(loppukeskustelu.lahiesimiesHyvaksynyt).isTrue()
        assertThat(loppukeskustelu.lahikouluttajanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(loppukeskustelu.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(loppukeskustelu.koejaksonPaattymispaiva).isEqualTo(aiempiPaiva)
        verify(loppukeskustelut).save(loppukeskustelu)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahiesimiehen palautus poistaa kouluttajan kuittauksen asettamatta uutta paivaa`(aika: Paivamaaratapaus) {
        val loppukeskustelu = valmisteleLoppukeskustelu().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = "Tarkenna jatkotoimenpiteet"

        luoPalvelu(aika).update(lomake, "lahiesimies")

        assertThat(loppukeskustelu.lahikouluttajanKuittausaika).isNull()
        assertThat(loppukeskustelu.lahikouluttajaHyvaksynyt).isFalse()
        assertThat(loppukeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(loppukeskustelu.lahiesimiesHyvaksynyt).isFalse()
        assertThat(loppukeskustelu.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(loppukeskustelu.koejaksonPaattymispaiva).isEqualTo(aiempiPaiva)
        assertThat(loppukeskustelu.korjausehdotus).isEqualTo("Tarkenna jatkotoimenpiteet")
        verify(loppukeskustelut).save(loppukeskustelu)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `sama lahikouluttaja ja lahiesimies kuittaa molemmat roolit kellon paivalla`(aika: Paivamaaratapaus) {
        val loppukeskustelu = valmisteleLoppukeskustelu().apply { lahiesimies = lahikouluttaja }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(loppukeskustelu.lahikouluttajanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(loppukeskustelu.lahiesimiehenKuittausaika).isEqualTo(aika.odotettu)
        assertThat(loppukeskustelu.lahikouluttajaHyvaksynyt).isTrue()
        assertThat(loppukeskustelu.lahiesimiesHyvaksynyt).isTrue()
        verify(loppukeskustelut, times(2)).save(loppukeskustelu)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttaja ei muuta hyvaksytyn loppukeskustelun kuittauspaivia`(aika: Paivamaaratapaus) {
        val loppukeskustelu = valmisteleLoppukeskustelu().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
            lahiesimiesHyvaksynyt = true
            lahiesimiehenKuittausaika = aiempiPaiva
        }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(loppukeskustelu.lahikouluttajanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(loppukeskustelu.lahiesimiehenKuittausaika).isEqualTo(aiempiPaiva)
        verify(loppukeskustelut, never()).save(any<KoejaksonLoppukeskustelu>())
        verifyNoInteractions(ilmoitukset)
    }

    private fun valmisteleLoppukeskustelu(): KoejaksonLoppukeskustelu {
        val loppukeskustelu = KoejaksonLoppukeskustelu(
            id = 1L,
            lahikouluttaja = lahikouluttaja,
            lahiesimies = lahiesimies,
            opintooikeus = Opintooikeus(erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = erikoistuja)),
            erikoistuvanKuittausaika = aiempiPaiva,
            koejaksonPaattymispaiva = aiempiPaiva
        )
        whenever(loppukeskustelut.findById(1L)).thenReturn(Optional.of(loppukeskustelu))
        whenever(loppukeskustelut.save(any<KoejaksonLoppukeskustelu>())).thenAnswer { it.arguments[0] }
        whenever(loppukeskusteluMapper.toEntity(lomake)).thenReturn(muutokset)
        whenever(loppukeskusteluMapper.toDto(loppukeskustelu)).thenReturn(lomake)
        listOf(lahikouluttaja, lahiesimies, erikoistuja).forEach { kayttaja ->
            whenever(kayttajat.findById(kayttaja.id!!)).thenReturn(Optional.of(kayttaja))
        }
        return loppukeskustelu
    }

    private fun luoPalvelu(aika: Paivamaaratapaus) = KoejaksonLoppukeskusteluServiceImpl(
        koejaksonLoppukeskusteluRepository = loppukeskustelut,
        koejaksonLoppukeskusteluMapper = loppukeskusteluMapper,
        mailService = ilmoitukset,
        kayttajaRepository = kayttajat,
        opintooikeusRepository = mock(),
        koejaksonAloituskeskusteluRepository = mock(),
        koejaksonValiarviointiRepository = mock(),
        koejaksonKehittamistoimenpiteetRepository = mock(),
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

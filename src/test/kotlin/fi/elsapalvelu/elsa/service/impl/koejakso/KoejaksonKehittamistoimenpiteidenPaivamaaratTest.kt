package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKehittamistoimenpiteet
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKehittamistoimenpiteetRepository
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKehittamistoimenpiteetDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKehittamistoimenpiteetMapper
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

class KoejaksonKehittamistoimenpiteidenPaivamaaratTest {
    private val toimenpiteetRepository = mock<KoejaksonKehittamistoimenpiteetRepository>()
    private val toimenpiteetMapper = mock<KoejaksonKehittamistoimenpiteetMapper>()
    private val kayttajat = mock<KayttajaRepository>()
    private val ilmoitukset = mock<MailService>()
    private val lahikouluttaja = Kayttaja(id = 2L, user = User(id = "lahikouluttaja"))
    private val lahiesimies = Kayttaja(id = 3L, user = User(id = "lahiesimies"))
    private val erikoistuja = Kayttaja(id = 4L, user = User(id = "erikoistuja"))
    private val aiempiPaiva: LocalDate = LocalDate.of(2019, 1, 1)
    private val lomake = KoejaksonKehittamistoimenpiteetDTO(id = 1L)
    private val muutokset = KoejaksonKehittamistoimenpiteet()

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttajan uusi kuittaus kayttaa kellon paivaa ja poistaa korjausehdotuksen`(aika: Paivamaaratapaus) {
        val kehittamistoimenpiteet = valmisteleKehittamistoimenpiteet().apply {
            lahikouluttajanKuittausaika = aiempiPaiva
            korjausehdotus = "Tarkenna kehittämistoimenpiteet"
        }
        muutokset.kehittamistoimenpiteetRiittavat = false

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(kehittamistoimenpiteet.lahikouluttajanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(kehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isTrue()
        assertThat(kehittamistoimenpiteet.lahiesimiehenKuittausaika).isNull()
        assertThat(kehittamistoimenpiteet.korjausehdotus).isNull()
        assertThat(kehittamistoimenpiteet.kehittamistoimenpiteetRiittavat).isFalse()
        verify(toimenpiteetRepository).save(kehittamistoimenpiteet)
    }

    @ParameterizedTest
    @MethodSource("hyvaksymistapaukset")
    fun `lahiesimiehen hyvaksynta kayttaa kellon paivaa ja sailyttaa aiemmat kuittaukset`(tapaus: Hyvaksymistapaus) {
        val kehittamistoimenpiteet = valmisteleKehittamistoimenpiteet().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = tapaus.korjausehdotus

        luoPalvelu(tapaus.aika).update(lomake, "lahiesimies")

        assertThat(kehittamistoimenpiteet.lahiesimiehenKuittausaika).isEqualTo(tapaus.aika.odotettu)
        assertThat(kehittamistoimenpiteet.lahiesimiesHyvaksynyt).isTrue()
        assertThat(kehittamistoimenpiteet.lahikouluttajanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(kehittamistoimenpiteet.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        verify(toimenpiteetRepository).save(kehittamistoimenpiteet)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahiesimiehen palautus poistaa kouluttajan kuittauksen asettamatta uutta paivaa`(aika: Paivamaaratapaus) {
        val kehittamistoimenpiteet = valmisteleKehittamistoimenpiteet().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = "Tarkenna kehittämistoimenpiteet"

        luoPalvelu(aika).update(lomake, "lahiesimies")

        assertThat(kehittamistoimenpiteet.lahikouluttajanKuittausaika).isNull()
        assertThat(kehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isFalse()
        assertThat(kehittamistoimenpiteet.lahiesimiehenKuittausaika).isNull()
        assertThat(kehittamistoimenpiteet.lahiesimiesHyvaksynyt).isFalse()
        assertThat(kehittamistoimenpiteet.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(kehittamistoimenpiteet.korjausehdotus).isEqualTo("Tarkenna kehittämistoimenpiteet")
        verify(toimenpiteetRepository).save(kehittamistoimenpiteet)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `sama lahikouluttaja ja lahiesimies kuittaa molemmat roolit kellon paivalla`(aika: Paivamaaratapaus) {
        val kehittamistoimenpiteet = valmisteleKehittamistoimenpiteet().apply { lahiesimies = lahikouluttaja }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(kehittamistoimenpiteet.lahikouluttajanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(kehittamistoimenpiteet.lahiesimiehenKuittausaika).isEqualTo(aika.odotettu)
        assertThat(kehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isTrue()
        assertThat(kehittamistoimenpiteet.lahiesimiesHyvaksynyt).isTrue()
        verify(toimenpiteetRepository, times(2)).save(kehittamistoimenpiteet)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttaja ei muuta hyvaksyttyjen kehittamistoimenpiteiden kuittauspaivia`(aika: Paivamaaratapaus) {
        val kehittamistoimenpiteet = valmisteleKehittamistoimenpiteet().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
            lahiesimiesHyvaksynyt = true
            lahiesimiehenKuittausaika = aiempiPaiva
        }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(kehittamistoimenpiteet.lahikouluttajanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(kehittamistoimenpiteet.lahiesimiehenKuittausaika).isEqualTo(aiempiPaiva)
        verify(toimenpiteetRepository, never()).save(any<KoejaksonKehittamistoimenpiteet>())
        verifyNoInteractions(ilmoitukset)
    }

    private fun valmisteleKehittamistoimenpiteet(): KoejaksonKehittamistoimenpiteet {
        val kehittamistoimenpiteet = KoejaksonKehittamistoimenpiteet(
            id = 1L,
            lahikouluttaja = lahikouluttaja,
            lahiesimies = lahiesimies,
            opintooikeus = Opintooikeus(erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = erikoistuja)),
            erikoistuvanKuittausaika = aiempiPaiva
        )
        whenever(toimenpiteetRepository.findById(1L)).thenReturn(Optional.of(kehittamistoimenpiteet))
        whenever(toimenpiteetRepository.save(any<KoejaksonKehittamistoimenpiteet>())).thenAnswer { it.arguments[0] }
        whenever(toimenpiteetMapper.toEntity(lomake)).thenReturn(muutokset)
        whenever(toimenpiteetMapper.toDto(kehittamistoimenpiteet)).thenReturn(lomake)
        listOf(lahikouluttaja, lahiesimies, erikoistuja).forEach { kayttaja ->
            whenever(kayttajat.findById(kayttaja.id!!)).thenReturn(Optional.of(kayttaja))
        }
        return kehittamistoimenpiteet
    }

    private fun luoPalvelu(aika: Paivamaaratapaus) = KoejaksonKehittamistoimenpiteetServiceImpl(
        koejaksonKehittamistoimenpiteetRepository = toimenpiteetRepository,
        koejaksonKehittamistoimenpiteetMapper = toimenpiteetMapper,
        mailService = ilmoitukset,
        kayttajaRepository = kayttajat,
        opintooikeusRepository = mock(),
        koejaksonValiarviointiRepository = mock(),
        kouluttajavaltuutusService = mock(),
        opintooikeusService = mock(),
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

package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonAloituskeskusteluDTO
import fi.elsapalvelu.elsa.service.kayttaja.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonAloituskeskusteluMapper
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

class KoejaksonAloituskeskustelunPaivamaaratTest {
    private val aloituskeskustelut = mock<KoejaksonAloituskeskusteluRepository>()
    private val aloituskeskusteluMapper = mock<KoejaksonAloituskeskusteluMapper>()
    private val kayttajat = mock<KayttajaRepository>()
    private val ilmoitukset = mock<MailService>()
    private val lahikouluttaja = Kayttaja(id = 2L, user = User(id = "lahikouluttaja"))
    private val lahiesimies = Kayttaja(id = 3L, user = User(id = "lahiesimies"))
    private val erikoistuja = Kayttaja(id = 4L, user = User(id = "erikoistuja"))
    private val aiempiPaiva: LocalDate = LocalDate.of(2019, 1, 1)
    private val erikoistujat = mock<ErikoistuvaLaakariRepository>()
    private val opintooikeudet = mock<OpintooikeusRepository>()
    private val valtuutukset = mock<KouluttajavaltuutusService>()
    private val lomake = KoejaksonAloituskeskusteluDTO(id = 1L)
    private val muutokset = KoejaksonAloituskeskustelu()

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttajan hyvaksynta kayttaa kellon paivaa`(aika: Paivamaaratapaus) {
        val aloituskeskustelu = valmisteleAloituskeskustelu().apply {
            lahikouluttajanKuittausaika = aiempiPaiva
        }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(aloituskeskustelu.lahikouluttajanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(aloituskeskustelu.lahikouluttajaHyvaksynyt).isTrue()
        assertThat(aloituskeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(aloituskeskustelu.korjausehdotus).isNull()
        verify(aloituskeskustelut).save(aloituskeskustelu)
    }

    @ParameterizedTest
    @MethodSource("hyvaksymistapaukset")
    fun `lahiesimiehen hyvaksynta kayttaa kellon paivaa ja sailyttaa aiemmat kuittaukset`(tapaus: Hyvaksymistapaus) {
        val aloituskeskustelu = valmisteleAloituskeskustelu().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = tapaus.korjausehdotus

        luoPalvelu(tapaus.aika).update(lomake, "lahiesimies")

        assertThat(aloituskeskustelu.lahiesimiehenKuittausaika).isEqualTo(tapaus.aika.odotettu)
        assertThat(aloituskeskustelu.lahiesimiesHyvaksynyt).isTrue()
        assertThat(aloituskeskustelu.lahikouluttajanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(aloituskeskustelu.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        verify(aloituskeskustelut).save(aloituskeskustelu)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahiesimiehen palautus poistaa kouluttajan kuittauksen asettamatta uutta paivaa`(aika: Paivamaaratapaus) {
        val aloituskeskustelu = valmisteleAloituskeskustelu().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
        }
        muutokset.korjausehdotus = "Tarkenna tavoitteet"

        luoPalvelu(aika).update(lomake, "lahiesimies")

        assertThat(aloituskeskustelu.lahikouluttajanKuittausaika).isNull()
        assertThat(aloituskeskustelu.lahikouluttajaHyvaksynyt).isFalse()
        assertThat(aloituskeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(aloituskeskustelu.lahiesimiesHyvaksynyt).isFalse()
        assertThat(aloituskeskustelu.erikoistuvanKuittausaika).isNull()
        assertThat(aloituskeskustelu.lahetetty).isFalse()
        assertThat(aloituskeskustelu.korjausehdotus).isEqualTo("Tarkenna tavoitteet")
        verify(aloituskeskustelut).save(aloituskeskustelu)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `sama lahikouluttaja ja lahiesimies kuittaa molemmat roolit kellon paivalla`(aika: Paivamaaratapaus) {
        val aloituskeskustelu = valmisteleAloituskeskustelu().apply { lahiesimies = lahikouluttaja }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(aloituskeskustelu.lahikouluttajanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(aloituskeskustelu.lahiesimiehenKuittausaika).isEqualTo(aika.odotettu)
        assertThat(aloituskeskustelu.lahikouluttajaHyvaksynyt).isTrue()
        assertThat(aloituskeskustelu.lahiesimiesHyvaksynyt).isTrue()
        verify(aloituskeskustelut, times(2)).save(aloituskeskustelu)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttaja ei muuta hyvaksytyn aloituskeskustelun kuittauspaivia`(aika: Paivamaaratapaus) {
        val aloituskeskustelu = valmisteleAloituskeskustelu().apply {
            lahikouluttajaHyvaksynyt = true
            lahikouluttajanKuittausaika = aiempiPaiva
            lahiesimiesHyvaksynyt = true
            lahiesimiehenKuittausaika = aiempiPaiva
        }

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(aloituskeskustelu.lahikouluttajanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(aloituskeskustelu.lahiesimiehenKuittausaika).isEqualTo(aiempiPaiva)
        verify(aloituskeskustelut, never()).save(any<KoejaksonAloituskeskustelu>())
        verifyNoInteractions(ilmoitukset)
    }

    @ParameterizedTest
    @MethodSource("luontitapaukset")
    fun `luonti asettaa erikoistuvan kuittauspaivan vain lahetettaessa`(tapaus: Luontitapaus) {
        valmisteleAloituskeskustelu()
        lomake.lahetetty = tapaus.lahetetty
        muutokset.lahetetty = tapaus.lahetetty == true
        muutokset.korjausehdotus = "Korjaa tiedot"

        luoPalvelu(tapaus.aika).create(lomake, 11L)

        assertThat(muutokset.erikoistuvanKuittausaika).isEqualTo(if (tapaus.lahetetty == true) tapaus.aika.odotettu else null)
        assertThat(muutokset.korjausehdotus).isNull()
        verify(aloituskeskustelut).save(muutokset)
        if (tapaus.lahetetty != true) verifyNoInteractions(ilmoitukset, valtuutukset)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `erikoistuvan uudelleenlahetys paivittaa kuittauspaivan ja poistaa korjausehdotuksen`(aika: Paivamaaratapaus) {
        val aloituskeskustelu = valmisteleAloituskeskustelu().apply { korjausehdotus = "Korjaa tiedot" }
        muutokset.lahetetty = true
        muutokset.koejaksonAlkamispaiva = aiempiPaiva

        luoPalvelu(aika).update(lomake, "erikoistuja")

        assertThat(aloituskeskustelu.erikoistuvanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(aloituskeskustelu.korjausehdotus).isNull()
        assertThat(aloituskeskustelu.koejaksonAlkamispaiva).isEqualTo(aiempiPaiva)
        verify(aloituskeskustelut).save(aloituskeskustelu)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `luonnoksen tallennus ei muuta aiempaa kuittauspaivaa`(aika: Paivamaaratapaus) {
        val aloituskeskustelu = valmisteleAloituskeskustelu().apply { korjausehdotus = "Korjaa tiedot" }
        muutokset.lahetetty = false

        luoPalvelu(aika).update(lomake, "erikoistuja")

        assertThat(aloituskeskustelu.erikoistuvanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(aloituskeskustelu.korjausehdotus).isEqualTo("Korjaa tiedot")
        verifyNoInteractions(ilmoitukset, valtuutukset)
        verify(aloituskeskustelut).save(aloituskeskustelu)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lahikouluttajan palautus poistaa erikoistuvan kuittauspaivan`(aika: Paivamaaratapaus) {
        val aloituskeskustelu = valmisteleAloituskeskustelu().apply { lahetetty = true }
        muutokset.korjausehdotus = "Korjaa tiedot"

        luoPalvelu(aika).update(lomake, "lahikouluttaja")

        assertThat(aloituskeskustelu.erikoistuvanKuittausaika).isNull()
        assertThat(aloituskeskustelu.lahikouluttajanKuittausaika).isNull()
        assertThat(aloituskeskustelu.lahetetty).isFalse()
        assertThat(aloituskeskustelu.korjausehdotus).isEqualTo("Korjaa tiedot")
        verify(aloituskeskustelut).save(aloituskeskustelu)
    }

    private fun valmisteleAloituskeskustelu(): KoejaksonAloituskeskustelu {
        val aloituskeskustelu = KoejaksonAloituskeskustelu(
            id = 1L,
            lahikouluttaja = lahikouluttaja,
            lahiesimies = lahiesimies,
            opintooikeus = Opintooikeus(id = 11L, erikoistuvaLaakari = ErikoistuvaLaakari(id = 8L, kayttaja = erikoistuja)),
            erikoistuvanKuittausaika = aiempiPaiva
        )
        whenever(aloituskeskustelut.findById(1L)).thenReturn(Optional.of(aloituskeskustelu))
        whenever(aloituskeskustelut.save(any<KoejaksonAloituskeskustelu>())).thenAnswer { it.arguments[0] }
        whenever(aloituskeskusteluMapper.toEntity(lomake)).thenReturn(muutokset)
        whenever(aloituskeskusteluMapper.toDto(any<KoejaksonAloituskeskustelu>())).thenReturn(lomake)
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.of(aloituskeskustelu.opintooikeus!!))
        whenever(erikoistujat.findOneByKayttajaUserId("erikoistuja")).thenReturn(aloituskeskustelu.opintooikeus!!.erikoistuvaLaakari)
        muutokset.id = 1L
        muutokset.lahikouluttaja = lahikouluttaja
        muutokset.lahiesimies = lahiesimies
        listOf(lahikouluttaja, lahiesimies, erikoistuja).forEach { kayttaja ->
            whenever(kayttajat.findById(kayttaja.id!!)).thenReturn(Optional.of(kayttaja))
        }
        return aloituskeskustelu
    }

    private fun luoPalvelu(aika: Paivamaaratapaus) = KoejaksonAloituskeskusteluServiceImpl(
        koejaksonAloituskeskusteluRepository = aloituskeskustelut,
        koejaksonAloituskeskusteluMapper = aloituskeskusteluMapper,
        mailService = ilmoitukset,
        kayttajaRepository = kayttajat,
        opintooikeusRepository = opintooikeudet,
        erikoistuvaLaakariRepository = erikoistujat,
        userRepository = mock(),
        kouluttajavaltuutusService = valtuutukset,
        opintooikeusService = mock(),
        pdfTextFieldValidator = mock(),
        clock = aika.kello()
    )

    data class Paivamaaratapaus(val hetki: String, val aikavyohyke: String, val odotettu: LocalDate) {
        fun kello(): Clock = Clock.fixed(Instant.parse(hetki), ZoneId.of(aikavyohyke))
    }

    data class Hyvaksymistapaus(val aika: Paivamaaratapaus, val korjausehdotus: String?)

    data class Luontitapaus(val aika: Paivamaaratapaus, val lahetetty: Boolean?)

    companion object {
        @JvmStatic
        fun luontitapaukset(): List<Luontitapaus> = paivamaaratapaukset().flatMap { aika ->
            listOf(null, false, true).map { lahetetty -> Luontitapaus(aika, lahetetty) }
        }

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

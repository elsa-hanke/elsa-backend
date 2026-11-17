package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksynta
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaMapper
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class TerveyskeskuskoulutusjaksonPaivamaaratTest {
    private val hyvaksynnat = mock<TerveyskeskuskoulutusjaksonHyvaksyntaRepository>()
    private val kayttajat = mock<KayttajaRepository>()
    private val tyoskentelyjaksot = mock<TyoskentelyjaksoRepository>()
    private val hyvaksyntaMapper = mock<TerveyskeskuskoulutusjaksonHyvaksyntaMapper>()
    private val virkailija = Kayttaja(id = 2L, user = User(id = "virkailija"))
    private val vastuuhenkilo = Kayttaja(id = 3L, user = User(id = "vastuuhenkilo"))
    private val aiempiPaiva: LocalDate = LocalDate.of(2019, 1, 1)

    @ParameterizedTest
    @MethodSource("kasittelytapaukset")
    fun `virkailijan hyvaksynta kayttaa kellon paivaa ja poistaa korjausehdotukset`(tapaus: Kasittelytapaus) {
        val hyvaksynta = valmisteleHyvaksynta(tapaus).apply {
            virkailijanKorjausehdotus = "Korjaa tiedot"
            vastuuhenkilonKorjausehdotus = "Tarkenna tiedot"
            virkailijanKuittausaika = aiempiPaiva
        }

        kasittele(tapaus, true)

        assertThat(hyvaksynta.virkailijanKuittausaika).isEqualTo(tapaus.odotettu)
        assertThat(hyvaksynta.virkailijaHyvaksynyt).isTrue()
        assertThat(hyvaksynta.virkailija).isSameAs(virkailija)
        assertThat(hyvaksynta.virkailijanKorjausehdotus).isNull()
        assertThat(hyvaksynta.vastuuhenkilonKorjausehdotus).isNull()
        assertThat(hyvaksynta.lisatiedotVirkailijalta).isEqualTo("Lisätiedot")
        verify(hyvaksynnat).save(hyvaksynta)
    }

    @ParameterizedTest
    @MethodSource("kasittelytapaukset")
    fun `vastuuhenkilon hyvaksynta kayttaa kellon paivaa ja sailyttaa virkailijan kuittauksen`(tapaus: Kasittelytapaus) {
        val hyvaksynta = valmisteleHyvaksynta(tapaus).apply {
            virkailijaHyvaksynyt = true
            virkailijanKuittausaika = aiempiPaiva
        }
        val tyoskentelyjakso = Tyoskentelyjakso(id = 4L)
        whenever(tyoskentelyjaksot.findAllByOpintooikeusIdAndTyoskentelypaikkaTyyppiAndKaytannonKoulutus(any(), any(), any()))
            .thenReturn(listOf(tyoskentelyjakso))

        kasittele(tapaus, false)

        assertThat(hyvaksynta.vastuuhenkilonKuittausaika).isEqualTo(tapaus.odotettu)
        assertThat(hyvaksynta.vastuuhenkiloHyvaksynyt).isTrue()
        assertThat(hyvaksynta.vastuuhenkilo).isSameAs(vastuuhenkilo)
        assertThat(hyvaksynta.virkailijanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon).isTrue()
        verify(tyoskentelyjaksot).save(tyoskentelyjakso)
        verify(hyvaksynnat).save(hyvaksynta)
    }

    @ParameterizedTest
    @MethodSource("kasittelytapaukset")
    fun `virkailijan palautus ei aseta kuittauspaivaa`(tapaus: Kasittelytapaus) {
        val hyvaksynta = valmisteleHyvaksynta(tapaus)

        kasittele(tapaus, true, "Korjaa tiedot")

        assertThat(hyvaksynta.virkailijanKuittausaika).isNull()
        assertThat(hyvaksynta.vastuuhenkilonKuittausaika).isNull()
        assertThat(hyvaksynta.virkailijaHyvaksynyt).isFalse()
        assertThat(hyvaksynta.erikoistujaLahettanyt).isFalse()
        assertThat(hyvaksynta.virkailijanKorjausehdotus).isEqualTo("Korjaa tiedot")
        verifyNoInteractions(tyoskentelyjaksot)
        verify(hyvaksynnat).save(hyvaksynta)
    }

    @ParameterizedTest
    @MethodSource("kasittelytapaukset")
    fun `vastuuhenkilon palautus poistaa virkailijan kuittauksen asettamatta uutta paivaa`(tapaus: Kasittelytapaus) {
        val hyvaksynta = valmisteleHyvaksynta(tapaus).apply {
            virkailija = this@TerveyskeskuskoulutusjaksonPaivamaaratTest.virkailija
            virkailijaHyvaksynyt = true
            virkailijanKuittausaika = aiempiPaiva
            lisatiedotVirkailijalta = "Lisätiedot"
        }

        kasittele(tapaus, false, "Korjaa tiedot")

        assertThat(hyvaksynta.virkailijanKuittausaika).isNull()
        assertThat(hyvaksynta.vastuuhenkilonKuittausaika).isNull()
        assertThat(hyvaksynta.virkailija).isNull()
        assertThat(hyvaksynta.virkailijaHyvaksynyt).isFalse()
        assertThat(hyvaksynta.vastuuhenkiloHyvaksynyt).isFalse()
        assertThat(hyvaksynta.erikoistujaLahettanyt).isFalse()
        assertThat(hyvaksynta.lisatiedotVirkailijalta).isNull()
        assertThat(hyvaksynta.vastuuhenkilonKorjausehdotus).isEqualTo("Korjaa tiedot")
        verifyNoInteractions(tyoskentelyjaksot)
        verify(hyvaksynnat).save(hyvaksynta)
    }

    private fun valmisteleHyvaksynta(tapaus: Kasittelytapaus): TerveyskeskuskoulutusjaksonHyvaksynta {
        val hyvaksynta = TerveyskeskuskoulutusjaksonHyvaksynta(
            id = 1L,
            erikoistujaLahettanyt = true,
            opintooikeus = Opintooikeus(id = 11L, yliopisto = Yliopisto(id = 7L), erikoisala = Erikoisala(id = if (tapaus.yek) YEK_ERIKOISALA_ID else 2L))
        )
        whenever(kayttajat.findOneByUserId("virkailija")).thenReturn(Optional.of(virkailija))
        whenever(kayttajat.findOneByUserId("vastuuhenkilo")).thenReturn(Optional.of(vastuuhenkilo))
        whenever(kayttajat.findOneByAuthoritiesYliopistoAndVastuuhenkilonTehtavatyyppi(
            listOf(VASTUUHENKILO), 7L,
            if (tapaus.yek) VastuuhenkilonTehtavatyyppiEnum.YEK_TERVEYSKESKUSKOULUTUSJAKSO
            else VastuuhenkilonTehtavatyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSOJEN_HYVAKSYMINEN
        )).thenReturn(vastuuhenkilo)
        whenever(hyvaksynnat.findById(1L)).thenReturn(Optional.of(hyvaksynta))
        whenever(hyvaksynnat.save(any<TerveyskeskuskoulutusjaksonHyvaksynta>())).thenAnswer { it.arguments[0] }
        whenever(hyvaksyntaMapper.toDto(hyvaksynta)).thenReturn(TerveyskeskuskoulutusjaksonHyvaksyntaDTO())
        return hyvaksynta
    }

    private fun kasittele(tapaus: Kasittelytapaus, virkailijanKasittely: Boolean, korjausehdotus: String? = null) {
        val kello = Clock.fixed(Instant.parse(tapaus.hetki), ZoneId.of(tapaus.aikavyohyke))
        val palvelu = TerveyskeskuskoulutusjaksonHyvaksyntaServiceImpl(
            terveyskeskuskoulutusjaksonHyvaksyntaRepository = hyvaksynnat,
            opintooikeusRepository = mock(),
            kayttajaRepository = kayttajat,
            tyoskentelyjaksoRepository = tyoskentelyjaksot,
            terveyskeskuskoulutusjaksonHyvaksyntaMapper = hyvaksyntaMapper,
            kayttajaMapper = mock(),
            tyoskentelyjaksoMapper = mock(),
            tyoskentelyjaksonPituusCounterService = mock(),
            terveyskeskuskoulutusjaksonHyvaksyntaQueryService = mock(),
            mailService = mock(),
            tyoskentelyjaksoService = mock(),
            applicationProperties = mock(),
            erikoistuvaLaakariService = mock(),
            clock = kello
        )
        palvelu.update(if (virkailijanKasittely) "virkailija" else "vastuuhenkilo", virkailijanKasittely, 1L, korjausehdotus, "Lisätiedot", null, null)
    }

    data class Kasittelytapaus(val hetki: String, val aikavyohyke: String, val odotettu: LocalDate, val yek: Boolean)

    companion object {
        @JvmStatic
        fun kasittelytapaukset(): List<Kasittelytapaus> = listOf(false, true).flatMap { yek ->
            listOf(
                Kasittelytapaus("2020-02-29T22:30:00Z", "UTC", LocalDate.of(2020, 2, 29), yek),
                Kasittelytapaus("2020-02-29T22:30:00Z", "Europe/Helsinki", LocalDate.of(2020, 3, 1), yek),
                Kasittelytapaus("2020-12-31T22:30:00Z", "Europe/Helsinki", LocalDate.of(2021, 1, 1), yek)
            )
        }
    }
}

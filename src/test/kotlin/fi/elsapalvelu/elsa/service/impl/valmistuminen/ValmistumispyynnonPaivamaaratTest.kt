package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyynnonTarkistusRepository
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.service.dto.valmistuminen.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusUpdateDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoOsaamisenArviointiFormDTO
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyynnonTarkistusMapper
import fi.elsapalvelu.elsa.service.mapper.valmistuminen.ValmistumispyyntoMapper
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ValmistumispyynnonPaivamaaratTest {
    private val pyynnot = mock<ValmistumispyyntoRepository>()
    private val tarkistukset = mock<ValmistumispyynnonTarkistusRepository>()
    private val pyyntoMapper = mock<ValmistumispyyntoMapper>()
    private val tarkistusMapper = mock<ValmistumispyynnonTarkistusMapper>()
    private val osapuolet = mock<ValmistumispyynnonOsapuoliService>()
    private val ilmoitukset = mock<ValmistumispyynnonIlmoitusService>()
    private val viimeistely = mock<ValmistumispyynnonViimeistelyService>()
    private val aiempiPaiva: LocalDate = LocalDate.of(2019, 1, 1)
    private val yliopisto = Yliopisto(id = 7L, nimi = YliopistoEnum.TURUN_YLIOPISTO)
    private val kasittelija = Kayttaja(id = 3L, user = User(id = "kasittelija"), yliopistot = mutableSetOf(yliopisto))

    @ParameterizedTest
    @MethodSource("lahetystapaukset")
    fun `uuden EL ja YEK pyynnon kuittauspaiva tulee kellosta`(tapaus: Lahetystapaus) {
        val opintooikeus = luoPyynto(tapaus.yek).opintooikeus!!
        whenever(osapuolet.haeOpintooikeus(11L)).thenReturn(opintooikeus)
        whenever(osapuolet.haeOsaamisenArvioija(7L, 2L)).thenReturn(kasittelija)
        val palvelu = luoPalvelu(tapaus.aika.kello())

        val tulos = palvelu.create(11L, UusiValmistumispyyntoDTO())

        val tallennettu = argumentCaptor<Valmistumispyynto>()
        verify(pyynnot).save(tallennettu.capture())
        assertThat(tallennettu.firstValue.erikoistujanKuittausaika).isEqualTo(tapaus.aika.odotettu)
        assertThat(tulos.tila).isEqualTo(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA)
        if (tapaus.yek) verify(ilmoitukset).lahetaIlmoitusVirkailijanTarkastuksesta(tallennettu.firstValue)
        else verify(ilmoitukset).lahetaIlmoitusUudestaValmistumispyynnosta(kasittelija.user!!, tallennettu.firstValue)
    }

    @ParameterizedTest
    @MethodSource("uudelleenlahetystapaukset")
    fun `uudelleenlahetys paivittaa kuittauspaivan ja sailyttaa EL YEK palautusten kasittelyn`(tapaus: Lahetystapaus) {
        val pyynto = luoPyynto(tapaus.yek).apply {
            vastuuhenkiloOsaamisenArvioijaKuittausaika = if (tapaus.osaaminenArvioitu) aiempiPaiva else null
            vastuuhenkiloOsaamisenArvioijaPalautusaika = aiempiPaiva
            vastuuhenkiloOsaamisenArvioijaKorjausehdotus = "Korjaa osaamisen tiedot"
            virkailijanPalautusaika = aiempiPaiva
        }
        whenever(osapuolet.haeOpintooikeus(11L)).thenReturn(pyynto.opintooikeus!!)
        whenever(osapuolet.haeValmistumispyyntoOpintooikeudella(11L)).thenReturn(pyynto)
        whenever(osapuolet.haeOsaamisenArvioija(7L, 2L)).thenReturn(kasittelija)

        luoPalvelu(tapaus.aika.kello()).update(11L, UusiValmistumispyyntoDTO(selvitysVanhentuneistaSuorituksista = "Selvitys"))

        assertThat(pyynto.erikoistujanKuittausaika).isEqualTo(tapaus.aika.odotettu)
        assertThat(pyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika).isNull()
        assertThat(pyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).isNull()
        assertThat(pyynto.virkailijanPalautusaika).isEqualTo(if (tapaus.yek || tapaus.osaaminenArvioitu) null else aiempiPaiva)
        assertThat(pyynto.selvitysVanhentuneistaSuorituksista).isEqualTo("Selvitys")
        verify(pyynnot).save(pyynto)
        if (tapaus.yek || tapaus.osaaminenArvioitu) verify(ilmoitukset).lahetaIlmoitusVirkailijanTarkastuksesta(pyynto)
        else verify(ilmoitukset).lahetaIlmoitusUudestaValmistumispyynnosta(kasittelija.user!!, pyynto)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `osaamisen arvioijan hyvaksynta kayttaa kellon paivaa`(aika: Paivamaaratapaus) {
        val pyynto = luoPyynto().apply { virkailijanPalautusaika = aiempiPaiva }
        valmisteleKasittely(pyynto)

        luoPalvelu(aika.kello()).updateOsaamisenArviointiByOsaamisenArvioijaUserId(
            1L, "kasittelija", ValmistumispyyntoOsaamisenArviointiFormDTO(osaaminenRiittavaValmistumiseen = true)
        )

        assertThat(pyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika).isEqualTo(aika.odotettu)
        assertThat(pyynto.virkailijanPalautusaika).isNull()
        assertThat(pyynto.vastuuhenkiloOsaamisenArvioija).isSameAs(kasittelija)
        verify(ilmoitukset).lahetaIlmoitusVirkailijanTarkastuksesta(pyynto)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `osaamisen arvioijan palautus kayttaa kellon paivaa ja poistaa erikoistujan kuittauksen`(aika: Paivamaaratapaus) {
        val pyynto = luoPyynto()
        valmisteleKasittely(pyynto)

        luoPalvelu(aika.kello()).updateOsaamisenArviointiByOsaamisenArvioijaUserId(
            1L, "kasittelija", ValmistumispyyntoOsaamisenArviointiFormDTO(osaaminenRiittavaValmistumiseen = false, korjausehdotus = "Korjaa")
        )

        assertThat(pyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika).isEqualTo(aika.odotettu)
        assertThat(pyynto.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).isEqualTo("Korjaa")
        assertThat(pyynto.erikoistujanKuittausaika).isNull()
        verify(ilmoitukset).lahetaIlmoitusPalautuksesta(pyynto)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lopullisen hyvaksyjan kuittauspaiva on asetettu ennen viimeistelya`(aika: Paivamaaratapaus) {
        val pyynto = luoPyynto()
        valmisteleKasittely(pyynto)
        doAnswer {
            assertThat(pyynto.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(aika.odotettu)
            null
        }.whenever(viimeistely).viimeistele(1L, pyynto.valmistumispyynnonTarkistus!!, pyynto, yliopisto.nimi)

        luoPalvelu(aika.kello()).updateValmistumispyyntoByHyvaksyjaUserId(1L, "kasittelija", ValmistumispyyntoHyvaksyntaFormDTO())

        assertThat(pyynto.vastuuhenkiloHyvaksyjaKuittausaika).isEqualTo(aika.odotettu)
        assertThat(pyynto.vastuuhenkiloHyvaksyja).isSameAs(kasittelija)
        verify(pyynnot).findByIdForHyvaksynta(1L)
        verify(pyynnot).save(pyynto)
        verify(viimeistely).viimeistele(1L, pyynto.valmistumispyynnonTarkistus!!, pyynto, yliopisto.nimi)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `lopullisen hyvaksyjan palautus kayttaa kellon paivaa ja poistaa aiemmat kuittaukset`(aika: Paivamaaratapaus) {
        val pyynto = luoPyynto().apply { virkailijanKuittausaika = aiempiPaiva }
        valmisteleKasittely(pyynto)

        luoPalvelu(aika.kello()).updateValmistumispyyntoByHyvaksyjaUserId(
            1L, "kasittelija", ValmistumispyyntoHyvaksyntaFormDTO(korjausehdotus = "Korjaa")
        )

        assertThat(pyynto.vastuuhenkiloHyvaksyjaPalautusaika).isEqualTo(aika.odotettu)
        assertThat(pyynto.erikoistujanKuittausaika).isNull()
        assertThat(pyynto.virkailijanKuittausaika).isNull()
        assertThat(pyynto.vastuuhenkiloHyvaksyjaKorjausehdotus).isEqualTo("Korjaa")
        verify(ilmoitukset).lahetaIlmoitusPalautuksesta(pyynto, ilmoitaYliopistolle = true)
        verifyNoInteractions(viimeistely)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `virkailijan hyvaksynta kayttaa samaa kellon paivaa`(aika: Paivamaaratapaus) {
        val pyynto = luoPyynto().apply { vastuuhenkiloHyvaksyjaPalautusaika = aiempiPaiva }
        valmisteleKasittely(pyynto)

        luoPalvelu(aika.kello()).updateTarkistusByVirkailijaUserId(
            1L, "kasittelija", ValmistumispyynnonTarkistusUpdateDTO(keskenerainen = false), null
        )

        assertThat(pyynto.virkailijanKuittausaika).isEqualTo(aika.odotettu)
        assertThat(pyynto.vastuuhenkiloHyvaksyjaPalautusaika).isNull()
        verify(pyynnot).save(pyynto)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `virkailijan palautus kayttaa samaa kellon paivaa`(aika: Paivamaaratapaus) {
        val pyynto = luoPyynto()
        valmisteleKasittely(pyynto)

        luoPalvelu(aika.kello()).updateTarkistusByVirkailijaUserId(
            1L, "kasittelija", ValmistumispyynnonTarkistusUpdateDTO(keskenerainen = false, korjausehdotus = "Korjaa"), null
        )

        assertThat(pyynto.virkailijanPalautusaika).isEqualTo(aika.odotettu)
        assertThat(pyynto.erikoistujanKuittausaika).isNull()
        verify(ilmoitukset).lahetaIlmoitusPalautuksesta(pyynto)
    }

    @ParameterizedTest
    @MethodSource("paivamaaratapaukset")
    fun `keskeneraisen tarkistuksen tallennus ei muuta kuittaus tai palautuspaivia`(aika: Paivamaaratapaus) {
        val pyynto = luoPyynto().apply { virkailijanKuittausaika = aiempiPaiva; virkailijanPalautusaika = aiempiPaiva }
        valmisteleKasittely(pyynto)

        luoPalvelu(aika.kello()).updateTarkistusByVirkailijaUserId(
            1L, "kasittelija", ValmistumispyynnonTarkistusUpdateDTO(keskenerainen = true), null
        )

        assertThat(pyynto.virkailijanKuittausaika).isEqualTo(aiempiPaiva)
        assertThat(pyynto.virkailijanPalautusaika).isEqualTo(aiempiPaiva)
        assertThat(pyynto.erikoistujanKuittausaika).isEqualTo(aiempiPaiva)
        verifyNoInteractions(ilmoitukset)
    }

    private fun luoPyynto(yek: Boolean = false) = Valmistumispyynto(
        id = 1L,
        erikoistujanKuittausaika = aiempiPaiva,
        opintooikeus = Opintooikeus(
            id = 11L, yliopisto = yliopisto, erikoisala = Erikoisala(id = if (yek) YEK_ERIKOISALA_ID else 2L),
            erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = Kayttaja(user = User(id = "erikoistuja")))
        )
    )

    private fun valmisteleKasittely(pyynto: Valmistumispyynto) {
        val tarkistus = ValmistumispyynnonTarkistus(id = 4L, valmistumispyynto = pyynto)
        pyynto.valmistumispyynnonTarkistus = tarkistus
        whenever(osapuolet.haeKayttaja("kasittelija")).thenReturn(kasittelija)
        whenever(osapuolet.haeYliopisto(kasittelija)).thenReturn(yliopisto)
        whenever(osapuolet.haeValmistumispyynto(eq(1L), eq(kasittelija), eq(7L), any())).thenReturn(pyynto)
        whenever(osapuolet.onkoLopullinenHyvaksyntaAvoin(pyynto)).thenReturn(true)
        whenever(pyynnot.findByIdForHyvaksynta(1L)).thenReturn(pyynto)
        whenever(tarkistukset.findByValmistumispyyntoId(1L)).thenReturn(tarkistus)
        whenever(tarkistukset.findByValmistumispyyntoIdAndValmistumispyyntoOpintooikeusYliopistoId(1L, 7L)).thenReturn(tarkistus)
        whenever(tarkistusMapper.toDto(tarkistus)).thenReturn(ValmistumispyynnonTarkistusDTO(valmistumispyynto = ValmistumispyyntoDTO()))
    }

    private fun luoPalvelu(kello: Clock): ValmistumispyyntoServiceImpl {
        whenever(pyynnot.save(any<Valmistumispyynto>())).thenAnswer { it.getArgument<Valmistumispyynto>(0) }
        whenever(pyyntoMapper.toDto(any<Valmistumispyynto>())).thenReturn(ValmistumispyyntoDTO())
        return ValmistumispyyntoServiceImpl(
            valmistumispyyntoRepository = pyynnot,
            valmistumispyyntoQueryService = mock(),
            valmistumispyyntoMapper = pyyntoMapper,
            valmistumispyyntoOsaamisenArviointiMapper = mock(),
            clock = kello,
            valmistumispyynnonTarkistusRepository = tarkistukset,
            valmistumispyynnonTarkistusMapper = tarkistusMapper,
            valmistumispyynnonTarkistusUpdateMapper = mock(),
            erikoistuvaLaakariService = mock(),
            vanhentumisService = mock(),
            tilaService = mock(),
            osapuoliService = osapuolet,
            arviointienTilaService = mock(),
            viimeistelyService = viimeistely,
            ilmoitusService = ilmoitukset,
            tarkistusService = mock()
        )
    }

    data class Paivamaaratapaus(val hetki: String, val aikavyohyke: String, val odotettu: LocalDate) {
        fun kello(): Clock = Clock.fixed(Instant.parse(hetki), ZoneId.of(aikavyohyke))
        override fun toString() = "$hetki / $aikavyohyke"
    }

    data class Lahetystapaus(val aika: Paivamaaratapaus, val yek: Boolean, val osaaminenArvioitu: Boolean = false) {
        override fun toString() = "$aika / yek=$yek / osaaminenArvioitu=$osaaminenArvioitu"
    }

    companion object {
        @JvmStatic
        fun paivamaaratapaukset(): List<Paivamaaratapaus> = listOf(
            Paivamaaratapaus("2020-02-29T22:30:00Z", "UTC", LocalDate.of(2020, 2, 29)),
            Paivamaaratapaus("2020-02-29T22:30:00Z", "Europe/Helsinki", LocalDate.of(2020, 3, 1)),
            Paivamaaratapaus("2020-12-31T22:30:00Z", "Europe/Helsinki", LocalDate.of(2021, 1, 1))
        )

        @JvmStatic
        fun lahetystapaukset(): List<Lahetystapaus> = paivamaaratapaukset().flatMap { aika ->
            listOf(false, true).map { yek -> Lahetystapaus(aika, yek) }
        }

        @JvmStatic
        fun uudelleenlahetystapaukset(): List<Lahetystapaus> = lahetystapaukset().flatMap { tapaus ->
            listOf(false, true).map { arvioitu -> tapaus.copy(osaaminenArvioitu = arvioitu) }
        }
    }
}

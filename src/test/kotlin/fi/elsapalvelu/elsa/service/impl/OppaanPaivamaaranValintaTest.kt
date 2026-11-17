package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavaKokonaisuus
import fi.elsapalvelu.elsa.domain.arviointi.ArvioitavanKokonaisuudenKategoria
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.repository.arviointi.ArvioitavaKokonaisuusRepository
import fi.elsapalvelu.elsa.repository.arviointi.ArvioitavanKokonaisuudenKategoriaRepository
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavaKokonaisuusDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.impl.arviointi.ArvioitavaKokonaisuusServiceImpl
import fi.elsapalvelu.elsa.service.impl.arviointi.ArvioitavanKokonaisuudenKategoriaServiceImpl
import fi.elsapalvelu.elsa.service.impl.valmistuminen.ValmistumispyynnonArviointienTilaService
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavaKokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.arviointi.ArvioitavanKokonaisuudenKategoriaMapper
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.suoritteet.Suorite
import fi.elsapalvelu.elsa.domain.suoritteet.SuoritteenKategoria
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoriteRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritteenKategoriaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.PoissaolonSyyRepository
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoriteDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritteenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.PoissaolonSyyDTO
import fi.elsapalvelu.elsa.service.impl.suoritteet.SuoriteServiceImpl
import fi.elsapalvelu.elsa.service.impl.suoritteet.SuoritteenKategoriaServiceImpl
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoriteMapper
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritteenKategoriaMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.PoissaolonSyyMapper
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class OppaanPaivamaaranValintaTest {
    private val opintooikeudet = mock<OpintooikeusRepository>()
    private val poissaolonSyyt = mock<PoissaolonSyyRepository>()
    private val suoritteet = mock<SuoriteRepository>()
    private val kategoriat = mock<SuoritteenKategoriaRepository>()
    private val poissaolonSyyMapper = mock<PoissaolonSyyMapper>()
    private val suoriteMapper = mock<SuoriteMapper>()
    private val kategoriaMapper = mock<SuoritteenKategoriaMapper>()

    private val arvioitavatKokonaisuudet = mock<ArvioitavaKokonaisuusRepository>()
    private val arviointikategoriat = mock<ArvioitavanKokonaisuudenKategoriaRepository>()
    private val kokonaisuusMapper = mock<ArvioitavaKokonaisuusMapper>()
    private val arviointikategoriaMapper = mock<ArvioitavanKokonaisuudenKategoriaMapper>()

    @ParameterizedTest
    @MethodSource("hakutapaukset")
    fun `poissaolon syiden voimassaolo kayttaa oppaan paivaa tai kellon paivaa`(tapaus: Hakutapaus) {
        valmisteleOpintooikeus(tapaus)
        val syy = PoissaolonSyy(id = 2L)
        val syyDto = PoissaolonSyyDTO(id = 2L)
        whenever(poissaolonSyyt.findAllByValid(tapaus.odotettu())).thenReturn(listOf(syy))
        whenever(poissaolonSyyMapper.toDto(syy)).thenReturn(syyDto)

        val tulos = poissaolopalvelu(tapaus.kello()).findAllByOpintooikeusId(11L)

        assertThat(tulos).containsExactly(syyDto)
        verify(poissaolonSyyt).findAllByValid(tapaus.odotettu())
    }

    @ParameterizedTest
    @MethodSource("hakutapaukset")
    fun `suoritteiden voimassaolo kayttaa oppaan paivaa tai kellon paivaa`(tapaus: Hakutapaus) {
        valmisteleOpintooikeus(tapaus)
        val suorite = Suorite(id = 3L)
        val suoriteDto = SuoriteDTO(id = 3L)
        whenever(suoritteet.findAllByValid(tapaus.odotettu())).thenReturn(listOf(suorite))
        whenever(suoriteMapper.toDto(suorite)).thenReturn(suoriteDto)

        val tulos = suoritepalvelu(tapaus.kello()).findAllOpintooikeusId(11L)

        assertThat(tulos).containsExactly(suoriteDto)
        verify(suoritteet).findAllByValid(tapaus.odotettu())
    }

    @ParameterizedTest
    @MethodSource("hakutapaukset")
    fun `voimassa olevat kategoriat haetaan oppaan tai kellon paivan ja erikoisalan mukaan`(tapaus: Hakutapaus) {
        valmisteleOpintooikeus(tapaus)
        val kategoria = SuoritteenKategoria(id = 4L)
        val kategoriaDto = SuoritteenKategoriaDTO(id = 4L)
        whenever(kategoriat.findAllByErikoisalaIdAndValid(7L, tapaus.odotettu())).thenReturn(listOf(kategoria))
        whenever(kategoriaMapper.toDto(kategoria)).thenReturn(kategoriaDto)

        val tulos = kategoriapalvelu(tapaus.kello()).findAllByOpintooikeusId(11L)

        assertThat(tulos).containsExactly(kategoriaDto)
        verify(kategoriat).findAllByErikoisalaIdAndValid(7L, tapaus.odotettu())
    }

    @ParameterizedTest
    @MethodSource("hakutapaukset")
    fun `paattyneet kategoriat haetaan oppaan tai kellon paivan ja erikoisalan mukaan`(tapaus: Hakutapaus) {
        valmisteleOpintooikeus(tapaus)
        val kategoria = SuoritteenKategoria(id = 4L)
        val kategoriaDto = SuoritteenKategoriaDTO(id = 4L)
        whenever(kategoriat.findAllByErikoisalaIdAndExpired(7L, tapaus.odotettu())).thenReturn(listOf(kategoria))
        whenever(kategoriaMapper.toDto(kategoria)).thenReturn(kategoriaDto)

        val tulos = kategoriapalvelu(tapaus.kello()).findAllExpiredByOpintooikeusId(11L)

        assertThat(tulos).containsExactly(kategoriaDto)
        verify(kategoriat).findAllByErikoisalaIdAndExpired(7L, tapaus.odotettu())
    }

    @ParameterizedTest
    @MethodSource("hakutapaukset")
    fun `arvioitavat kokonaisuudet haetaan oppaan tai kellon paivan mukaan`(tapaus: Hakutapaus) {
        valmisteleOpintooikeus(tapaus)
        val kokonaisuus = ArvioitavaKokonaisuus(id = 8L)
        val kokonaisuusDto = ArvioitavaKokonaisuusDTO(id = 8L)
        whenever(arvioitavatKokonaisuudet.findAllByErikoisalaIdAndValid(7L, tapaus.odotettu())).thenReturn(listOf(kokonaisuus))
        whenever(kokonaisuusMapper.toDto(kokonaisuus)).thenReturn(kokonaisuusDto)

        val tulos = kokonaisuuspalvelu(tapaus.kello()).findAllByOpintooikeusId(11L)

        verify(arvioitavatKokonaisuudet).findAllByErikoisalaIdAndValid(7L, tapaus.odotettu())
        assertThat(tulos).containsExactly(kokonaisuusDto)
    }

    @ParameterizedTest
    @MethodSource("hakutapaukset")
    fun `arviointikategoriat haetaan oppaan tai kellon paivan mukaan`(tapaus: Hakutapaus) {
        valmisteleOpintooikeus(tapaus)
        val kategoria = ArvioitavanKokonaisuudenKategoria(id = 9L)
        val kategoriaDto = ArvioitavanKokonaisuudenKategoriaDTO(id = 9L)
        whenever(arviointikategoriat.findAllByErikoisalaIdAndValid(7L, tapaus.odotettu())).thenReturn(listOf(kategoria))
        whenever(arviointikategoriaMapper.toDto(kategoria)).thenReturn(kategoriaDto)

        val tulos = arviointikategoriapalvelu(tapaus.kello()).findAllByOpintooikeusId(11L)

        verify(arviointikategoriat).findAllByErikoisalaIdAndValid(7L, tapaus.odotettu())
        assertThat(tulos).containsExactly(kategoriaDto)
    }

    @ParameterizedTest
    @MethodSource("hakutapaukset")
    fun `valmistumisen arviointitila kayttaa oppaan tai kellon paivan kokonaisuuksia`(tapaus: Hakutapaus) {
        val opintooikeus = Opintooikeus(id = 11L, erikoisala = Erikoisala(id = 7L), osaamisenArvioinninOppaanPvm = tapaus.oppaanPaiva)
        whenever(arvioitavatKokonaisuudet.findAllByErikoisalaIdAndValid(7L, tapaus.odotettu()))
            .thenReturn(listOf(ArvioitavaKokonaisuus(id = 8L)))
        val palvelu = ValmistumispyynnonArviointienTilaService(arvioitavatKokonaisuudet, mock(), tapaus.kello())

        val tulos = palvelu.haeArviointienTila(Valmistumispyynto(opintooikeus = opintooikeus))

        verify(arvioitavatKokonaisuudet).findAllByErikoisalaIdAndValid(7L, tapaus.odotettu())
        assertThat(tulos.hasArvioitaviaKokonaisuuksiaWithoutArviointi).isTrue()
        assertThat(tulos.hasArvioitaviaKokonaisuuksiaWithArviointiLowerThanFour).isFalse()
    }

    @ParameterizedTest
    @MethodSource("sivutustapaukset")
    fun `sivutettu haku rajaa voimassaolon kellon paivalla`(tapaus: Sivutustapaus) {
        val sivutus = PageRequest.of(1, 5)
        val kokonaisuus = ArvioitavaKokonaisuus(id = 8L)
        val kokonaisuusDto = ArvioitavaKokonaisuusDTO(id = 8L)
        val sivu = PageImpl(listOf(kokonaisuus), sivutus, 12)
        if (tapaus.voimassaolevat == true) {
            whenever(arvioitavatKokonaisuudet.findAllByErikoisalaIdAndValid(eq(7L), any(), eq(sivutus))).thenReturn(sivu)
        } else {
            whenever(arvioitavatKokonaisuudet.findAllByErikoisalaIdAndExpired(eq(7L), any(), eq(sivutus))).thenReturn(sivu)
        }
        whenever(kokonaisuusMapper.toDto(kokonaisuus)).thenReturn(kokonaisuusDto)

        val tulos = kokonaisuuspalvelu(tapaus.aika.kello()).findAllByErikoisalaIdPaged(7L, tapaus.voimassaolevat, sivutus)

        if (tapaus.voimassaolevat == true) verify(arvioitavatKokonaisuudet).findAllByErikoisalaIdAndValid(7L, tapaus.aika.kellonPaiva, sivutus)
        else verify(arvioitavatKokonaisuudet).findAllByErikoisalaIdAndExpired(7L, tapaus.aika.kellonPaiva, sivutus)
        assertThat(tulos.content).containsExactly(kokonaisuusDto)
        assertThat(tulos.totalElements).isEqualTo(12)
        assertThat(tulos.number).isEqualTo(1)
    }

    @Test
    fun `puuttuva opintooikeus palauttaa tyhjat listat tekematta voimassaolohakuja`() {
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.empty())
        val kello = hakutapaukset().first().kello()

        assertThat(poissaolopalvelu(kello).findAllByOpintooikeusId(11L)).isEmpty()
        assertThat(suoritepalvelu(kello).findAllOpintooikeusId(11L)).isEmpty()
        assertThat(kategoriapalvelu(kello).findAllByOpintooikeusId(11L)).isEmpty()
        assertThat(kategoriapalvelu(kello).findAllExpiredByOpintooikeusId(11L)).isEmpty()
        assertThat(kokonaisuuspalvelu(kello).findAllByOpintooikeusId(11L)).isEmpty()
        assertThat(arviointikategoriapalvelu(kello).findAllByOpintooikeusId(11L)).isEmpty()
        verifyNoInteractions(arvioitavatKokonaisuudet, arviointikategoriat, kokonaisuusMapper, arviointikategoriaMapper)
        verifyNoInteractions(poissaolonSyyt, suoritteet, kategoriat, poissaolonSyyMapper, suoriteMapper, kategoriaMapper)
    }

    private fun valmisteleOpintooikeus(tapaus: Hakutapaus) {
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.of(
            Opintooikeus(id = 11L, erikoisala = Erikoisala(id = 7L), osaamisenArvioinninOppaanPvm = tapaus.oppaanPaiva)
        ))
    }

    private fun poissaolopalvelu(kello: Clock) = PoissaolonSyyServiceImpl(poissaolonSyyt, poissaolonSyyMapper, opintooikeudet, kello)

    private fun suoritepalvelu(kello: Clock) = SuoriteServiceImpl(
        suoriteRepository = suoritteet,
        opintooikeusRepository = opintooikeudet,
        suoriteMapper = suoriteMapper,
        suoriteWithErikoisalaMapper = mock(),
        suoritteenKategoriaWithErikoisalaMapper = mock(),
        suoritemerkintaRepository = mock(),
        clock = kello
    )

    private fun kategoriapalvelu(kello: Clock) = SuoritteenKategoriaServiceImpl(
        suoritteenKategoriaRepository = kategoriat,
        opintooikeusRepository = opintooikeudet,
        suoritteenKategoriaMapper = kategoriaMapper,
        suoritteenKategoriaSimpleMapper = mock(),
        suoritteenKategoriaWithErikoisalaMapper = mock(),
        clock = kello
    )

    private fun kokonaisuuspalvelu(kello: Clock) = ArvioitavaKokonaisuusServiceImpl(
        arvioitavaKokonaisuusRepository = arvioitavatKokonaisuudet,
        arvioitavaKokonaisuusMapper = kokonaisuusMapper,
        arvioitavaKokonaisuusWithErikoisalaMapper = mock(), opintooikeusRepository = opintooikeudet,
        arvioitavanKokonaisuudenKategoriaSimpleMapper = mock(), suoritusarviointiRepository = mock(), clock = kello
    )

    private fun arviointikategoriapalvelu(kello: Clock) = ArvioitavanKokonaisuudenKategoriaServiceImpl(
        arvioitavanKokonaisuudenKategoriaRepository = arviointikategoriat,
        arvioitavanKokonaisuudenKategoriaMapper = arviointikategoriaMapper,
        arvioitavanKokonaisuudenKategoriaSimpleMapper = mock(), arvioitavanKokonaisuudenKategoriaWithErikoisalaMapper = mock(),
        opintooikeusRepository = opintooikeudet, clock = kello
    )

    data class Sivutustapaus(val aika: Hakutapaus, val voimassaolevat: Boolean?)

    data class Hakutapaus(val hetki: String, val aikavyohyke: String, val kellonPaiva: LocalDate, val oppaanPaiva: LocalDate?) {
        fun kello(): Clock = Clock.fixed(Instant.parse(hetki), ZoneId.of(aikavyohyke))
        fun odotettu(): LocalDate = oppaanPaiva ?: kellonPaiva
    }

    companion object {
        @JvmStatic
        fun sivutustapaukset(): List<Sivutustapaus> = hakutapaukset().filter { it.oppaanPaiva == null }.flatMap { aika ->
            listOf(null, false, true).map { Sivutustapaus(aika, it) }
        }

        @JvmStatic
        fun hakutapaukset(): List<Hakutapaus> = listOf(null, LocalDate.of(2018, 8, 1)).flatMap { oppaanPaiva ->
            listOf(
                Hakutapaus("2020-02-29T22:30:00Z", "UTC", LocalDate.of(2020, 2, 29), oppaanPaiva),
                Hakutapaus("2020-02-29T22:30:00Z", "Europe/Helsinki", LocalDate.of(2020, 3, 1), oppaanPaiva),
                Hakutapaus("2020-12-31T22:30:00Z", "Europe/Helsinki", LocalDate.of(2021, 1, 1), oppaanPaiva)
            )
        }
    }
}

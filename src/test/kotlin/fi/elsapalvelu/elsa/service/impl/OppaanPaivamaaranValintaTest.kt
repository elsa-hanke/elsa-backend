package fi.elsapalvelu.elsa.service.impl

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

    @Test
    fun `puuttuva opintooikeus palauttaa tyhjat listat tekematta voimassaolohakuja`() {
        whenever(opintooikeudet.findById(11L)).thenReturn(Optional.empty())
        val kello = hakutapaukset().first().kello()

        assertThat(poissaolopalvelu(kello).findAllByOpintooikeusId(11L)).isEmpty()
        assertThat(suoritepalvelu(kello).findAllOpintooikeusId(11L)).isEmpty()
        assertThat(kategoriapalvelu(kello).findAllByOpintooikeusId(11L)).isEmpty()
        assertThat(kategoriapalvelu(kello).findAllExpiredByOpintooikeusId(11L)).isEmpty()
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

    data class Hakutapaus(val hetki: String, val aikavyohyke: String, val kellonPaiva: LocalDate, val oppaanPaiva: LocalDate?) {
        fun kello(): Clock = Clock.fixed(Instant.parse(hetki), ZoneId.of(aikavyohyke))
        fun odotettu(): LocalDate = oppaanPaiva ?: kellonPaiva
    }

    companion object {
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

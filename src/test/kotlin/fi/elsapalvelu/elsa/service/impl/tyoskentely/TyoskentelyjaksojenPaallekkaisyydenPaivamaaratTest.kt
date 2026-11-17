package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyajanPaallekkaisyystarkistus
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.tyoskentely.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TyoskentelyjaksojenPaallekkaisyydenPaivamaaratTest {
    private val jaksot = mock<TyoskentelyjaksoRepository>()
    private val keskeytykset = mock<KeskeytysaikaRepository>()

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `avoimen jakson tarkistus ei ulotu huomiseen paallekkaisyyteen`(aika: KellonTestitapaus) {
        val huomisenJakso = Tyoskentelyjakso(id = 2L, alkamispaiva = aika.paiva.plusDays(1), osaaikaprosentti = 100)
        whenever(jaksot.findAllByOpintooikeusUntilDateEagerWithRelationships(eq(11L), any())).thenReturn(listOf(huomisenJakso))
        val jakso = TyoskentelyjaksoDTO(alkamispaiva = aika.paiva, osaaikaprosentti = 50)

        assertThat(validaattori(aika).validateTyoskentelyjakso(11L, jakso)).isTrue()
        verify(jaksot).findAllByOpintooikeusUntilDateEagerWithRelationships(11L, aika.paiva)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `kellon paivan paallekkaisyys estaa avoimen jakson`(aika: KellonTestitapaus) {
        val nykyinenJakso = Tyoskentelyjakso(id = 2L, alkamispaiva = aika.paiva, osaaikaprosentti = 100)
        whenever(jaksot.findAllByOpintooikeusUntilDateEagerWithRelationships(eq(11L), any())).thenReturn(listOf(nykyinenJakso))

        assertThat(validaattori(aika).validateTyoskentelyjakso(11L, TyoskentelyjaksoDTO(alkamispaiva = aika.paiva, osaaikaprosentti = 1))).isFalse()
        verify(jaksot).findAllByOpintooikeusUntilDateEagerWithRelationships(11L, aika.paiva)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `tulevaisuudessa alkava avoin jakso ei viela vaadi paallekkaisyystarkistusta`(aika: KellonTestitapaus) {
        val jakso = Tyoskentelyjakso(id = 2L, alkamispaiva = aika.paiva.plusDays(1), osaaikaprosentti = 100)
        whenever(jaksot.findAllByOpintooikeusUntilDateEagerWithRelationships(eq(11L), any())).thenReturn(listOf(jakso))

        assertThat(validaattori(aika).validateTyoskentelyjakso(11L, TyoskentelyjaksoDTO(alkamispaiva = aika.paiva.plusDays(1), osaaikaprosentti = 100)))
            .isTrue()
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `keskeytyksen muokkaus rajataan kellon paivaan muuttamatta alkuperaista jaksoa`(aika: KellonTestitapaus) {
        val jakso = valmisteleKeskeytys(aika)
        val keskeytys = KeskeytysaikaDTO(id = 3L, alkamispaiva = aika.paiva, paattymispaiva = aika.paiva, poissaoloprosentti = 100,
            tyoskentelyjaksoId = 1L, tyoskentelyjakso = TyoskentelyjaksoDTO(id = 1L, alkamispaiva = aika.paiva, osaaikaprosentti = 100))

        assertThat(validaattori(aika).validateKeskeytysaika(11L, keskeytys)).isTrue()
        verify(jaksot).findAllByOpintooikeusUntilDateEagerWithRelationships(11L, aika.paiva)
        assertThat(jakso.keskeytykset.single().poissaoloprosentti).isEqualTo(50)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `keskeytyksen poiston tarkistus ei ulotu huomiseen eika poista alkuperaista keskeytysta`(aika: KellonTestitapaus) {
        val jakso = valmisteleKeskeytys(aika)

        assertThat(validaattori(aika).validateKeskeytysaikaDelete(11L, 3L)).isTrue()
        verify(jaksot).findAllByOpintooikeusUntilDateEagerWithRelationships(11L, aika.paiva)
        assertThat(jakso.keskeytykset).hasSize(1)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `annettu paattymispaiva ohjaa kaikkien tarkistusten hakurajaa`(aika: KellonTestitapaus) {
        val jakso = valmisteleKeskeytys(aika)
        val paattymispaiva = aika.paiva.minusDays(1)
        jakso.alkamispaiva = aika.paiva.minusDays(2)
        jakso.paattymispaiva = paattymispaiva
        whenever(jaksot.findAllByOpintooikeusUntilDateEagerWithRelationships(eq(11L), any())).thenReturn(listOf(jakso))
        val jaksoDto = TyoskentelyjaksoDTO(id = 1L, alkamispaiva = aika.paiva.minusDays(2), paattymispaiva = paattymispaiva, osaaikaprosentti = 100)
        val palvelu = validaattori(aika)

        assertThat(palvelu.validateTyoskentelyjakso(11L, jaksoDto)).isTrue()
        assertThat(palvelu.validateKeskeytysaika(11L, KeskeytysaikaDTO(tyoskentelyjakso = jaksoDto))).isTrue()
        assertThat(palvelu.validateKeskeytysaikaDelete(11L, 3L)).isTrue()
        verify(jaksot, times(3)).findAllByOpintooikeusUntilDateEagerWithRelationships(11L, paattymispaiva)
    }

    private fun valmisteleKeskeytys(aika: KellonTestitapaus): Tyoskentelyjakso {
        val jakso = Tyoskentelyjakso(id = 1L, alkamispaiva = aika.paiva, osaaikaprosentti = 100)
        val keskeytys = Keskeytysaika(id = 3L, alkamispaiva = aika.paiva, paattymispaiva = aika.paiva, poissaoloprosentti = 50,
            poissaolonSyy = PoissaolonSyy(vahennystyyppi = PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN), tyoskentelyjakso = jakso)
        jakso.keskeytykset.add(keskeytys)
        val huomisenJakso = Tyoskentelyjakso(id = 2L, alkamispaiva = aika.paiva.plusDays(1), osaaikaprosentti = 100)
        whenever(jaksot.findAllByOpintooikeusUntilDateEagerWithRelationships(eq(11L), any())).thenReturn(listOf(jakso, huomisenJakso))
        whenever(keskeytykset.findOneByIdAndTyoskentelyjaksoOpintooikeusId(3L, 11L)).thenReturn(keskeytys)
        return jakso
    }

    private fun validaattori(aika: KellonTestitapaus) = OverlappingTyoskentelyjaksoValidationServiceImpl(
        jaksot, keskeytykset, TyoskentelyajanPaallekkaisyystarkistus(TyoskentelyjaksonPituusCounterServiceImpl(aika.kello())), aika.kello()
    )
}

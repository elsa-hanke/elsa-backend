package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TyoskentelyjaksonPituudenPaivamaaratTest {
    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `avoin jakso lasketaan kellon paivaan osa aikaisuus ja poissaolot huomioiden`(aika: KellonTestitapaus) {
        val laskuri = TyoskentelyjaksonPituusCounterServiceImpl(aika.kello())
        val jakso = Tyoskentelyjakso(alkamispaiva = aika.paiva.minusDays(2), osaaikaprosentti = 50)

        assertThat(laskuri.calculateInDays(jakso, 0.25)).isEqualTo(1.25)
        assertThat(laskuri.calculateInDays(jakso, 100.0)).isZero()
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `tuleva paattymispaiva rajataan kellon paivaan`(aika: KellonTestitapaus) {
        val jakso = Tyoskentelyjakso(alkamispaiva = aika.paiva.minusDays(2), paattymispaiva = aika.paiva.plusDays(10), osaaikaprosentti = 50)

        assertThat(TyoskentelyjaksonPituusCounterServiceImpl(aika.kello()).calculateInDays(jakso, null)).isEqualTo(1.5)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `paattyneen jakson paattymispaiva sailyy ja laskenta sisaltaa molemmat rajapaivat`(aika: KellonTestitapaus) {
        val jakso = Tyoskentelyjakso(alkamispaiva = aika.paiva.minusDays(2), paattymispaiva = aika.paiva.minusDays(1), osaaikaprosentti = 50)

        assertThat(TyoskentelyjaksonPituusCounterServiceImpl(aika.kello()).calculateInDays(jakso, null)).isEqualTo(1.0)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `huomenna alkava avoin jakso ei kerry viela`(aika: KellonTestitapaus) {
        val jakso = Tyoskentelyjakso(alkamispaiva = aika.paiva.plusDays(1), osaaikaprosentti = 100)

        assertThat(TyoskentelyjaksonPituusCounterServiceImpl(aika.kello()).calculateInDays(jakso, null)).isZero()
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `avoin jakso rajaa hyvaksiluettavat vuodet kellon vuoteen`(aika: KellonTestitapaus) {
        val avoinJakso = Tyoskentelyjakso(alkamispaiva = LocalDate.of(2019, 12, 31))
        val tulevaJakso = Tyoskentelyjakso(alkamispaiva = LocalDate.of(2029, 1, 1), paattymispaiva = LocalDate.of(2030, 1, 1))
        val laskuri = TyoskentelyjaksonPituusCounterServiceImpl(aika.kello())

        val tulos = laskuri.getHyvaksiluettavatPerYearMap(listOf(avoinJakso, tulevaJakso))

        assertThat(tulos.keys).containsExactlyElementsOf((2019..aika.paiva.year).toList())
        assertThat(tulos.values).allMatch { it == 30.0 }
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `maaraaikaiset jaksot sailyttavat vuosirajat myos tulevaisuudessa`(aika: KellonTestitapaus) {
        val jakso = Tyoskentelyjakso(alkamispaiva = LocalDate.of(2029, 1, 1), paattymispaiva = LocalDate.of(2030, 1, 1))
        val laskuri = TyoskentelyjaksonPituusCounterServiceImpl(aika.kello())

        assertThat(laskuri.getHyvaksiluettavatPerYearMap(listOf(jakso))).containsOnlyKeys(2029, 2030)
        assertThat(laskuri.getHyvaksiluettavatPerYearMap(emptyList())).isEmpty()
    }
}

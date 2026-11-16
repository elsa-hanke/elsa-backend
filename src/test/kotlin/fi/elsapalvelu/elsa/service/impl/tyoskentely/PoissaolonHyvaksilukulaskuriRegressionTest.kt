package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

class PoissaolonHyvaksilukulaskuriRegressionTest {
    private val pituusLaskuri = TyoskentelyjaksonPituusCounterServiceImpl(java.time.Clock.systemDefaultZone())

    @Test
    fun `keston laskenta sailyttaa vuosittaisen saldon kun kertaluonteinen saldo loppuu vuodenvaihteessa`() {
        val poissaolonSyy = poissaolonSyy(1L, vahennetaanKerran = true)
        val saldo = HyvaksiluettavatCounterData(hyvaksiluettavatPerYearMap = mutableMapOf(2020 to 30.0, 2021 to 30.0))
        val keskeytysaika = Keskeytysaika(
            alkamispaiva = LocalDate.of(2020, 12, 31), paattymispaiva = LocalDate.of(2021, 1, 31),
            poissaoloprosentti = 100, poissaolonSyy = poissaolonSyy
        )

        val vahennettavatPaivat = pituusLaskuri.calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(keskeytysaika, 1.0, saldo, null)

        assertThat(vahennettavatPaivat).isEqualTo(2.0)
        assertThat(saldo.hyvaksiluettavatDays.getValue(poissaolonSyy)).isZero()
        assertThat(saldo.hyvaksiluettavatPerYearMap).containsEntry(2020, 29.0).containsEntry(2021, 1.0)
        val seuraavaKeskeytysaika = keskeytysaika.copy(
            alkamispaiva = LocalDate.of(2021, 2, 1), paattymispaiva = LocalDate.of(2021, 2, 2),
            poissaolonSyy = poissaolonSyy(2L, vahennetaanKerran = false)
        )
        assertThat(pituusLaskuri.calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(seuraavaKeskeytysaika, 1.0, saldo, null))
            .isEqualTo(1.0)
        assertThat(saldo.hyvaksiluettavatPerYearMap.getValue(2021)).isZero()
    }

    @Test
    fun `keston laskenta sailyttaa desimaalivahennyksen ja huomioi poissaolo- ja osaaikaprosentin`() {
        val saldo = HyvaksiluettavatCounterData(hyvaksiluettavatPerYearMap = mutableMapOf(2021 to 0.1))
        val keskeytysaika = Keskeytysaika(
            alkamispaiva = LocalDate.of(2021, 1, 1), paattymispaiva = LocalDate.of(2021, 1, 3),
            poissaoloprosentti = 50, poissaolonSyy = poissaolonSyy(1L, vahennetaanKerran = false)
        )

        assertThat(pituusLaskuri.calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(keskeytysaika, 0.2, saldo, null))
            .isEqualTo(0.20000000000000004)
        assertThat(saldo.hyvaksiluettavatPerYearMap.getValue(2021)).isZero()
    }

    @Test
    fun `laskennan katkaisu ennen poissaoloa sailyttaa saldot`() {
        val saldo = HyvaksiluettavatCounterData(hyvaksiluettavatPerYearMap = mutableMapOf(2021 to 30.0))
        val alkamispaiva = LocalDate.of(2021, 1, 1)
        val keskeytysaika = Keskeytysaika(
            alkamispaiva = alkamispaiva, paattymispaiva = alkamispaiva.plusDays(10),
            poissaoloprosentti = 100, poissaolonSyy = poissaolonSyy(1L, vahennetaanKerran = true)
        )

        assertThat(pituusLaskuri.calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(keskeytysaika, 1.0, saldo, alkamispaiva.minusDays(1)))
            .isZero()
        assertThat(saldo.hyvaksiluettavatDays).isEmpty()
        assertThat(saldo.hyvaksiluettavatPerYearMap).containsExactlyEntriesOf(mapOf(2021 to 30.0))
    }

    @Test
    fun `paallekkaisyysvalidointi sallii tasan tayden tyoajan vuosittaisen saldon loputtua`() {
        val alkamispaiva = LocalDate.of(2021, 1, 1)
        val paattymispaiva = alkamispaiva.plusDays(30)
        val tyoskentelyjakso = Tyoskentelyjakso(id = 1L, alkamispaiva = alkamispaiva, paattymispaiva = paattymispaiva, osaaikaprosentti = 100)
        tyoskentelyjakso.keskeytykset.add(Keskeytysaika(
            id = 1L, alkamispaiva = alkamispaiva, paattymispaiva = paattymispaiva,
            poissaoloprosentti = 100, poissaolonSyy = poissaolonSyy(1L, vahennetaanKerran = false), tyoskentelyjakso = tyoskentelyjakso
        ))
        val tyoskentelyjaksoRepository = mock<TyoskentelyjaksoRepository>()
        whenever(tyoskentelyjaksoRepository.findAllByOpintooikeusUntilDateEagerWithRelationships(1L, paattymispaiva)).thenReturn(listOf(tyoskentelyjakso))
        val validaattori = OverlappingTyoskentelyjaksoValidationServiceImpl(tyoskentelyjaksoRepository, mock(), pituusLaskuri, java.time.Clock.systemDefaultZone())

        assertThat(validaattori.validateTyoskentelyjakso(1L, TyoskentelyjaksoDTO(
            alkamispaiva = paattymispaiva, paattymispaiva = paattymispaiva, osaaikaprosentti = 100
        ))).isTrue()
        assertThat(validaattori.validateTyoskentelyjakso(1L, TyoskentelyjaksoDTO(
            alkamispaiva = paattymispaiva.minusDays(1), paattymispaiva = paattymispaiva, osaaikaprosentti = 100
        ))).isFalse()
    }

    private fun poissaolonSyy(id: Long, vahennetaanKerran: Boolean) = PoissaolonSyy(
        id = id, vahennystyyppi = PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI, vahennetaanKerran = vahennetaanKerran
    )
}

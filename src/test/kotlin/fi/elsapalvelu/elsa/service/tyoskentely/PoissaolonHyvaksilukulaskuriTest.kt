package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PoissaolonHyvaksilukulaskuriTest {
    @ParameterizedTest
    @CsvSource(
        "0.0, 30.0, 0.0, 30.0",
        "10.0, 30.0, 0.0, 20.0",
        "30.0, 30.0, 0.0, 0.0",
        "31.0, 30.0, 1.0, 0.0",
        "0.25, 0.1, 0.15, 0.0",
        "0.25, 0.0, 0.25, 0.0"
    )
    fun `keston laskenta jakaa poissaolon vuosittaisen saldon rajalla`(poissaolopaivat: Double, kaytettavissa: Double, vahennettavatPaivat: Double, jaljella: Double) {
        val saldo = saldo(kaytettavissa)

        assertThat(PoissaolonHyvaksilukulaskuri.laskeKestostaVahennettavatPaivat(poissaolopaivat, 2021, PoissaolonSyy(), saldo)).isEqualTo(vahennettavatPaivat)
        assertThat(saldo.hyvaksiluettavatPerYearMap.getValue(2021)).isEqualTo(jaljella)
        assertThat(saldo.hyvaksiluettavatDays).isEmpty()
    }

    @Test
    fun `kertaluonteinen saldo on yhteinen eri vuosille mutta erillinen eri poissaolon syille`() {
        val ensimmainenPoissaolonSyy = PoissaolonSyy(id = 1L, vahennetaanKerran = true)
        val toinenPoissaolonSyy = PoissaolonSyy(id = 2L, vahennetaanKerran = true)
        val saldo = saldo(30.0).apply { hyvaksiluettavatPerYearMap[2022] = 30.0 }

        assertThat(PoissaolonHyvaksilukulaskuri.laskeKestostaVahennettavatPaivat(20.0, 2021, ensimmainenPoissaolonSyy, saldo)).isZero()
        assertThat(PoissaolonHyvaksilukulaskuri.laskeKestostaVahennettavatPaivat(15.0, 2022, ensimmainenPoissaolonSyy, saldo)).isEqualTo(5.0)
        assertThat(saldo.hyvaksiluettavatPerYearMap).containsEntry(2021, 10.0).containsEntry(2022, 20.0)
        assertThat(saldo.hyvaksiluettavatDays.getValue(ensimmainenPoissaolonSyy)).isZero()
        assertThat(PoissaolonHyvaksilukulaskuri.laskeKestostaVahennettavatPaivat(5.0, 2022, toinenPoissaolonSyy, saldo)).isZero()
        assertThat(saldo.hyvaksiluettavatDays.getValue(toinenPoissaolonSyy)).isEqualTo(25.0)
    }

    @Test
    fun `paivittainen laskenta kuluttaa koko poissaolon molemmista saldoista vaikka kertaluonteinen saldo loppuu`() {
        val poissaolonSyy = PoissaolonSyy(id = 1L, vahennetaanKerran = true)
        val kestonLaskennanSaldo = saldo(1.0).apply { hyvaksiluettavatDays[poissaolonSyy] = 0.25 }
        val paivittaisenLaskennanSaldo = saldo(1.0).apply { hyvaksiluettavatDays[poissaolonSyy] = 0.25 }

        assertThat(PoissaolonHyvaksilukulaskuri.laskeKestostaVahennettavatPaivat(0.5, 2021, poissaolonSyy, kestonLaskennanSaldo)).isEqualTo(0.25)
        assertThat(PoissaolonHyvaksilukulaskuri.laskePaivittaisestaTyoskentelyajastaVahennettavaOsuus(0.5, 2021, poissaolonSyy, paivittaisenLaskennanSaldo)).isEqualTo(0.25)
        assertThat(kestonLaskennanSaldo.hyvaksiluettavatPerYearMap.getValue(2021)).isEqualTo(0.75)
        assertThat(paivittaisenLaskennanSaldo.hyvaksiluettavatPerYearMap.getValue(2021)).isEqualTo(0.5)
        assertThat(kestonLaskennanSaldo.hyvaksiluettavatDays.getValue(poissaolonSyy)).isZero()
        assertThat(paivittaisenLaskennanSaldo.hyvaksiluettavatDays.getValue(poissaolonSyy)).isZero()
        assertThat(PoissaolonHyvaksilukulaskuri.laskePaivittaisestaTyoskentelyajastaVahennettavaOsuus(0.5, 2021, poissaolonSyy, paivittaisenLaskennanSaldo)).isEqualTo(0.5)
        assertThat(paivittaisenLaskennanSaldo.hyvaksiluettavatPerYearMap.getValue(2021)).isZero()
    }

    @Test
    fun `vuosittainen saldo rajoittaa hyvaksilukua vaikka kertaluonteista saldoa on jaljella`() {
        val poissaolonSyy = PoissaolonSyy(id = 1L, vahennetaanKerran = true)
        val kestonLaskennanSaldo = saldo(5.0).apply { hyvaksiluettavatDays[poissaolonSyy] = 20.0 }
        val paivittaisenLaskennanSaldo = saldo(5.0).apply { hyvaksiluettavatDays[poissaolonSyy] = 20.0 }

        assertThat(PoissaolonHyvaksilukulaskuri.laskeKestostaVahennettavatPaivat(10.0, 2021, poissaolonSyy, kestonLaskennanSaldo)).isEqualTo(5.0)
        assertThat(PoissaolonHyvaksilukulaskuri.laskePaivittaisestaTyoskentelyajastaVahennettavaOsuus(10.0, 2021, poissaolonSyy, paivittaisenLaskennanSaldo)).isEqualTo(5.0)
        assertThat(kestonLaskennanSaldo.hyvaksiluettavatPerYearMap.getValue(2021)).isZero()
        assertThat(paivittaisenLaskennanSaldo.hyvaksiluettavatPerYearMap.getValue(2021)).isZero()
        assertThat(kestonLaskennanSaldo.hyvaksiluettavatDays.getValue(poissaolonSyy)).isEqualTo(15.0)
        assertThat(paivittaisenLaskennanSaldo.hyvaksiluettavatDays.getValue(poissaolonSyy)).isEqualTo(10.0)
    }

    @Test
    fun `paivittainen laskenta sailyttaa Double-tarkkuuden osittaisen saldon rajalla`() {
        val kestonLaskennanSaldo = saldo(0.1)
        val paivittaisenLaskennanSaldo = saldo(0.1)
        val poissaolonSyy = PoissaolonSyy()

        assertThat(PoissaolonHyvaksilukulaskuri.laskeKestostaVahennettavatPaivat(0.3, 2021, poissaolonSyy, kestonLaskennanSaldo)).isEqualTo(0.2)
        assertThat(PoissaolonHyvaksilukulaskuri.laskePaivittaisestaTyoskentelyajastaVahennettavaOsuus(0.3, 2021, poissaolonSyy, paivittaisenLaskennanSaldo))
            .isEqualTo(0.19999999999999998)
    }

    @Test
    fun `paivittainen laskenta alustaa vuosittaisen ja kertaluonteisen saldon uudelle vuodelle`() {
        val poissaolonSyy = PoissaolonSyy(id = 1L, vahennetaanKerran = true)
        val saldo = HyvaksiluettavatCounterData()

        assertThat(PoissaolonHyvaksilukulaskuri.laskePaivittaisestaTyoskentelyajastaVahennettavaOsuus(0.25, 2021, poissaolonSyy, saldo)).isZero()
        assertThat(saldo.hyvaksiluettavatPerYearMap.getValue(2021)).isEqualTo(29.75)
        assertThat(saldo.hyvaksiluettavatDays.getValue(poissaolonSyy)).isEqualTo(29.75)
    }

    private fun saldo(vuosittainenSaldo: Double) = HyvaksiluettavatCounterData(
        hyvaksiluettavatPerYearMap = mutableMapOf(2021 to vuosittainenSaldo)
    )
}

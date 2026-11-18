package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.service.constants.HYVAKSILUETTAVAT_DAYS
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Soveltaa vuosittaista ja tarvittaessa kertaluonteista hyvaksilukua kokonaisina tyopaivina laskettuun poissaoloon. */
object PoissaolonHyvaksilukulaskuri {
    fun laskeKestostaVahennettavatPaivat(
        poissaolopaivat: Double,
        vuosi: Int,
        poissaolonSyy: PoissaolonSyy,
        hyvaksiluettavienPaivienSaldo: HyvaksiluettavatCounterData
    ): Double {
        val hyvaksiluetutPaivat = kaytaHyvaksiluettaviaPaivia(
            poissaolopaivat, vuosi, poissaolonSyy, hyvaksiluettavienPaivienSaldo, kulutaKokoPoissaolo = false
        )
        // Keston laskennassa vahennettava osuus lasketaan nykyisen kaytannon mukaisesti desimaaliluvuilla.
        return if (hyvaksiluetutPaivat == 0.0) poissaolopaivat
        else BigDecimal.valueOf(poissaolopaivat).subtract(BigDecimal.valueOf(hyvaksiluetutPaivat)).toDouble()
    }

    fun laskePaivittaisestaTyoskentelyajastaVahennettavaOsuus(
        poissaolopaivat: Double,
        vuosi: Int,
        poissaolonSyy: PoissaolonSyy,
        hyvaksiluettavienPaivienSaldo: HyvaksiluettavatCounterData
    ): Double {
        hyvaksiluettavienPaivienSaldo.hyvaksiluettavatPerYearMap.putIfAbsent(vuosi, HYVAKSILUETTAVAT_DAYS)
        val hyvaksiluetutPaivat = kaytaHyvaksiluettaviaPaivia(
            poissaolopaivat, vuosi, poissaolonSyy, hyvaksiluettavienPaivienSaldo, kulutaKokoPoissaolo = true
        )
        // Sailytetaan Double-laskenta: paallekkaisyysvalidointi vertaa tyoskentelyajan osuutta suoraan arvoon 1.0.
        val erotus = hyvaksiluetutPaivat - poissaolopaivat
        return if (erotus < 0) abs(erotus) else 0.0
    }

    private fun kaytaHyvaksiluettaviaPaivia(
        poissaolopaivat: Double,
        vuosi: Int,
        poissaolonSyy: PoissaolonSyy,
        hyvaksiluettavienPaivienSaldo: HyvaksiluettavatCounterData,
        kulutaKokoPoissaolo: Boolean
    ): Double {
        val vuosittainenSaldo = hyvaksiluettavienPaivienSaldo.hyvaksiluettavatPerYearMap.getValue(vuosi)
        val kaytettavissaOlevatPaivat = if (poissaolonSyy.vahennetaanKerran) {
            val kertaluonteinenSaldo = hyvaksiluettavienPaivienSaldo.hyvaksiluettavatDays.getOrPut(poissaolonSyy) { HYVAKSILUETTAVAT_DAYS }
            min(vuosittainenSaldo, kertaluonteinenSaldo)
        } else vuosittainenSaldo
        val hyvaksiluetutPaivat = min(poissaolopaivat, kaytettavissaOlevatPaivat)
        // Keston laskenta kuluttaa vain hyvaksiluetut paivat. Paivittainen paallekkaisyysvalidointi
        // kuluttaa koko poissaolon myos silloin, kun kertaluonteinen saldo on loppunut.
        val kulutetutPaivat = if (kulutaKokoPoissaolo) poissaolopaivat else hyvaksiluetutPaivat
        hyvaksiluettavienPaivienSaldo.hyvaksiluettavatPerYearMap[vuosi] = max(0.0, vuosittainenSaldo - kulutetutPaivat)
        if (poissaolonSyy.vahennetaanKerran) {
            hyvaksiluettavienPaivienSaldo.hyvaksiluettavatDays[poissaolonSyy] = max(
                0.0, hyvaksiluettavienPaivienSaldo.hyvaksiluettavatDays.getValue(poissaolonSyy) - kulutetutPaivat
            )
        }
        return hyvaksiluetutPaivat
    }
}

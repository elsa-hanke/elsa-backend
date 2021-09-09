package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi.*
import fi.elsapalvelu.elsa.extensions.daysBetween
import fi.elsapalvelu.elsa.extensions.duringYears
import fi.elsapalvelu.elsa.extensions.endOfYearDate
import fi.elsapalvelu.elsa.extensions.startOfYearDate
import fi.elsapalvelu.elsa.service.TyoskentelyjaksonPituusCounterService
import fi.elsapalvelu.elsa.service.dto.HyvaksiluettavatCounterData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

private const val hyvaksiluettavatDays: Double = 30.0

@Service
@Transactional
class TyoskentelyjaksonPituusCounterServiceImpl : TyoskentelyjaksonPituusCounterService {

    override fun calculateInDays(
        tyoskentelyjakso: Tyoskentelyjakso,
        hyvaksiluettavatCounterData: HyvaksiluettavatCounterData
    ): Double {
        val tyoskentelyJaksoEndDate = tyoskentelyjakso.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
        // Lasketaan työskentelyjakson päivät
        val daysBetween = tyoskentelyjakso.alkamispaiva!!.daysBetween(tyoskentelyJaksoEndDate)

        // Ei huomioida tulevaisuuden jaksoja
        if (daysBetween > 0) {
            val tyoskentelyjaksoFactor = tyoskentelyjakso.osaaikaprosentti!!.toDouble() / 100.0
            var result = tyoskentelyjaksoFactor * daysBetween

            // Vähennetään keskeytykset
            tyoskentelyjakso.keskeytykset.map { keskeytysaika ->
                val keskeytysaikaDaysBetween = keskeytysaika.alkamispaiva!!.daysBetween(keskeytysaika.paattymispaiva!!)
                val keskeytysaikaProsentti = keskeytysaika.osaaikaprosentti!!.toDouble()
                // Keskeytysajan prosentti 0 % tarkoittaa kokopäiväpoissaoloa
                val keskeytysaikaFactor =
                    if (keskeytysaikaProsentti == 0.0) 1.0
                    else keskeytysaikaProsentti / 100.0
                // Kerrotaan myös työskentelyjakson osa-aikaprosentilla, koska esim. 50% poissaolo 50% mittaisesta
                // työpäivästä vähentää hyväksiluettavia päiviä kyseisen päivän osalta vain 0,25 päivää.
                val keskeytysaikaLength = keskeytysaikaFactor * tyoskentelyjaksoFactor * keskeytysaikaDaysBetween

                when (keskeytysaika.poissaolonSyy!!.vahennystyyppi!!) {
                    VAHENNETAAN_SUORAAN -> {
                        result -= keskeytysaikaLength
                    }
                    VAHENNETAAN_YLIMENEVA_AIKA -> {
                        val (amountOfReducedDays, hyvaksiLuettavatLeft) =
                            getAmountOfReducedDaysAndHyvaksiluettavatLeft(
                                keskeytysaikaLength,
                                hyvaksiluettavatCounterData.hyvaksiluettavatDays
                            )
                        result -= amountOfReducedDays
                        hyvaksiluettavatCounterData.hyvaksiluettavatDays = hyvaksiLuettavatLeft
                    }
                    VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI -> {
                        val keskeytysaikaMap = getKeskeytysaikaMap(
                            keskeytysaika,
                            keskeytysaikaFactor,
                            tyoskentelyjaksoFactor
                        )
                        keskeytysaikaMap.forEach {
                            val (amountOfReducedDays, hyvaksiLuettavatLeft) =
                                getAmountOfReducedDaysAndHyvaksiluettavatLeft(
                                    it.value,
                                    hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[it.key]!!
                                )
                            result -= amountOfReducedDays
                            hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[it.key] = hyvaksiLuettavatLeft
                        }
                    }
                }
            }

            // Koskaan ei summata negatiivisa arvoja laskuriin! (Esim. jos on kirjattu poissaolo useampaan kertaan)
            return max(0.0, result)
        }

        return 0.0
    }

    // Muodostetaan Map jossa avaimena työskentelyjakson vuosi ja arvona mahdollisten hyväksiluettavien
    // päivien määrä.
    override fun getHyvaksiluettavatPerYearMap(tyoskentelyjaksot: List<Tyoskentelyjakso>): MutableMap<Int, Double> {
        val hyvaksiLuettavatPerYearMap: MutableMap<Int, Double> = mutableMapOf()
        if (!tyoskentelyjaksot.any()) {
            return hyvaksiLuettavatPerYearMap
        }

        val min = tyoskentelyjaksot.minOf { it.alkamispaiva!! }
        // Jos mukana työskentelyjaksoja jotka yhä käynnissä, käytetään maksimi työskentelyjakson päättymispäivänä
        // tätä päivää.
        val max =
            if (tyoskentelyjaksot.any { it.paattymispaiva == null })
                LocalDate.now(ZoneId.systemDefault())
            else
                tyoskentelyjaksot.maxOf { it.paattymispaiva!! }
        val duringYears = min.duringYears(max)

        duringYears.forEach {
            hyvaksiLuettavatPerYearMap[it] = hyvaksiluettavatDays
        }

        return hyvaksiLuettavatPerYearMap
    }

    private fun getAmountOfReducedDaysAndHyvaksiluettavatLeft(
        keskeytysaikaLength: Double,
        hyvaksiluettavatLeft: Double
    ): Pair<Double, Double> {
        var amountOfReducedDays = 0.0
        var hyvaksiLuettavatLeftUpdated = hyvaksiluettavatLeft
        when {
            // Jäljellä olevia hyväksiluettavia päiviä ei ole jäljellä, joten vähennetään
            // keskeytysajan pituus.
            hyvaksiluettavatLeft == 0.0 -> {
                amountOfReducedDays = keskeytysaikaLength
            }
            // Keskeytysajan pituus on suurempi tai yhtä suuri kuin jäljellä olevien
            // hyväksiluettavien päivien määrä, joten vähennetään työskentelyjaksosta
            // keskeystysajan pituuden ja jäljellä olevien hyväksiluettavien päivien erotus sekä
            // asetetaan jäljellä olevien hyväksiluettavien päivien määrä nollaan.
            keskeytysaikaLength >= hyvaksiluettavatLeft -> {
                amountOfReducedDays = keskeytysaikaLength - hyvaksiluettavatLeft
                hyvaksiLuettavatLeftUpdated = 0.0
            }
            // Keskeytysajan pituus on pienempi kuin jäljellä olevien hyväksiluettavien päivien määrä,
            // vähennetään vain jäljellä olevien hyväksiluettavien päivien määrää.
            else -> {
                hyvaksiLuettavatLeftUpdated = hyvaksiluettavatLeft - keskeytysaikaLength
            }
        }
        return Pair(amountOfReducedDays, hyvaksiLuettavatLeftUpdated)
    }

    // Muodostetaan Map, jossa avaimena vuosi ja arvona kuinka monta päivää keskeytysajasta
    // kyseiselle vuodelle sijoittuu.
    private fun getKeskeytysaikaMap(
        keskeytysaika: Keskeytysaika,
        keskeytysaikaFactor: Double,
        tyoskentelyjaksoFactor: Double
    ): MutableMap<Int, Double> {
        val keskeytysaikaMap = mutableMapOf<Int, Double>()
        val keskeytysaikaDuringYears = keskeytysaika.alkamispaiva!!.duringYears(keskeytysaika.paattymispaiva!!)
        val first = keskeytysaikaDuringYears.first()
        val last = keskeytysaikaDuringYears.last()

        keskeytysaikaDuringYears.forEach {
            val dateFrom =
                if (it == first) keskeytysaika.alkamispaiva!!
                else it.startOfYearDate()
            val dateTo =
                if (it == last) keskeytysaika.paattymispaiva!!
                else it.endOfYearDate()
            val betweenDays = dateFrom.daysBetween(dateTo)
            // Kerrotaan myös työskentelyjakson osa-aikaprosentilla, koska esim. 50% poissaolo 50% mittaisesta
            // työpäivästä vähentää hyväksiluettavia päiviä kyseisen päivän osalta vain 0,25 päivää.
            keskeytysaikaMap[it] = betweenDays * keskeytysaikaFactor * tyoskentelyjaksoFactor
        }

        return keskeytysaikaMap
    }
}

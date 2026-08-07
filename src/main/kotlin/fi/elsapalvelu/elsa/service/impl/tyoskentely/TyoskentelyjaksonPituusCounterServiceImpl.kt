package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.required

import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyyTyyppi.*
import fi.elsapalvelu.elsa.extensions.daysBetween
import fi.elsapalvelu.elsa.extensions.duringYears
import fi.elsapalvelu.elsa.extensions.endOfYearDate
import fi.elsapalvelu.elsa.extensions.startOfYearDate
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksonPituusCounterService
import fi.elsapalvelu.elsa.service.constants.HYVAKSILUETTAVAT_DAYS
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

@Service
@Transactional
class TyoskentelyjaksonPituusCounterServiceImpl : TyoskentelyjaksonPituusCounterService {

    override fun calculateInDays(
        tyoskentelyjakso: Tyoskentelyjakso,
        vahennettavatPaivat: Double?
    ): Double {
        val now = LocalDate.now(ZoneId.systemDefault())
        val tyoskentelyJaksoEndDate =
            if (tyoskentelyjakso.paattymispaiva == null || tyoskentelyjakso.paattymispaiva.required() > now)
                now
            else
                tyoskentelyjakso.paattymispaiva.required()
        // Lasketaan työskentelyjakson päivät
        val daysBetween = tyoskentelyjakso.alkamispaiva.required().daysBetween(tyoskentelyJaksoEndDate)

        // Ei huomioida tulevaisuuden jaksoja
        if (daysBetween > 0) {
            val tyoskentelyjaksoFactor = tyoskentelyjakso.osaaikaprosentti.required().toDouble() / 100.0
            var result = tyoskentelyjaksoFactor * daysBetween

            // Vähennetään keskeytykset
            vahennettavatPaivat?.let {
                result -= it
            }

            // Koskaan ei summata negatiivisia arvoja laskuriin! (Esim. jos on kirjattu poissaolo useampaan kertaan)
            return max(0.0, result)
        }

        return 0.0
    }

    override fun calculateHyvaksiluettavatDaysLeft(
        tyoskentelyjaksot: List<Tyoskentelyjakso>,
        calculateUntilDate: LocalDate?
    ): HyvaksiluettavatCounterData {
        val hyvaksiluettavatCounterData = HyvaksiluettavatCounterData().apply {
            hyvaksiluettavatPerYearMap = getHyvaksiluettavatPerYearMap(tyoskentelyjaksot)
        }

        tyoskentelyjaksot.map { it.keskeytykset }.flatten().sortedBy { k -> k.alkamispaiva }
            .forEach {
                val tyoskentelyjaksoFactor =
                    it.tyoskentelyjakso?.osaaikaprosentti.required().toDouble() / 100.0

                calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(
                    it,
                    tyoskentelyjaksoFactor,
                    hyvaksiluettavatCounterData,
                    calculateUntilDate
                )
            }

        return hyvaksiluettavatCounterData
    }

    // Muodostetaan Map jossa avaimena työskentelyjakson vuosi ja arvona mahdollisten hyväksiluettavien
    // päivien määrä.
    override fun getHyvaksiluettavatPerYearMap(tyoskentelyjaksot: List<Tyoskentelyjakso>): MutableMap<Int, Double> {
        val hyvaksiLuettavatPerYearMap: MutableMap<Int, Double> = mutableMapOf()
        if (!tyoskentelyjaksot.any()) {
            return hyvaksiLuettavatPerYearMap
        }

        val min = tyoskentelyjaksot.minOf { it.alkamispaiva.required() }
        // Jos mukana työskentelyjaksoja jotka yhä käynnissä, käytetään maksimi työskentelyjakson päättymispäivänä
        // tätä päivää.
        val max =
            if (tyoskentelyjaksot.any { it.paattymispaiva == null })
                LocalDate.now(ZoneId.systemDefault())
            else
                tyoskentelyjaksot.maxOf { it.paattymispaiva.required() }
        val duringYears = min.duringYears(max)

        duringYears.forEach {
            hyvaksiLuettavatPerYearMap[it] = HYVAKSILUETTAVAT_DAYS
        }

        return hyvaksiLuettavatPerYearMap
    }

    override fun calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(
        keskeytysaika: Keskeytysaika,
        tyoskentelyjaksoFactor: Double,
        hyvaksiluettavatCounterData: HyvaksiluettavatCounterData,
        calculateUntilDate: LocalDate?
    ): Double {
        val endDate = getEndDate(keskeytysaika.paattymispaiva.required(), calculateUntilDate)
        val keskeytysaikaDaysBetween =
            keskeytysaika.alkamispaiva.required().daysBetween(endDate)

        if (keskeytysaikaDaysBetween < 1) return 0.0

        val keskeytysaikaProsentti = keskeytysaika.poissaoloprosentti.required().toDouble()
        val keskeytysaikaFactor = keskeytysaikaProsentti / 100.0
        // Kerrotaan myös työskentelyjakson osa-aikaprosentilla, koska esim. 50% poissaolo 50% mittaisesta
        // työpäivästä vähentää hyväksiluettavia päiviä kyseisen päivän osalta vain 0,25 päivää.
        val keskeytysaikaLength =
            keskeytysaikaFactor * tyoskentelyjaksoFactor * keskeytysaikaDaysBetween
        val vahennetaanKerran = keskeytysaika.poissaolonSyy.required().vahennetaanKerran

        when (keskeytysaika.poissaolonSyy.required().vahennystyyppi.required()) {
            VAHENNETAAN_SUORAAN -> {
                return keskeytysaikaLength
            }
            VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI -> {
                if (vahennetaanKerran) {
                    hyvaksiluettavatCounterData.hyvaksiluettavatDays.putIfAbsent(
                        keskeytysaika.poissaolonSyy.required(),
                        30.0
                    )
                }

                val keskeytysaikaMap = getKeskeytysaikaMap(
                    keskeytysaika.alkamispaiva.required(),
                    endDate,
                    keskeytysaikaFactor,
                    tyoskentelyjaksoFactor
                )
                var reducedDaysTotal = 0.0
                keskeytysaikaMap.forEach {
                    // Tarkistetaan hyväksiluettavat päivät vuosittaisesta määrästä ja
                    // vain kerran hyväksyttävien keskeytyksien (vanhempainvapaat) osalta
                    // poissaolokohtaisesta määrästä. Hyväksiluetaan näistä niin paljon kuin
                    // pystytään ja päivitetään molemmat laskurit.
                    val hyvaksiLuettavatLeft = if (vahennetaanKerran) min(
                        hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap.getValue(it.key),
                        hyvaksiluettavatCounterData.hyvaksiluettavatDays.getValue(keskeytysaika.poissaolonSyy.required())
                    ) else hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap.getValue(it.key)
                    val (amountOfReducedDays, hyvaksiluettavatUsed) = getAmountOfReducedDaysAndHyvaksiluettavatUsed(
                        it.value,
                        hyvaksiLuettavatLeft
                    )
                    if (vahennetaanKerran) {
                        hyvaksiluettavatCounterData.hyvaksiluettavatDays[keskeytysaika.poissaolonSyy.required()] =
                            max(
                                0.0,
                                hyvaksiluettavatCounterData.hyvaksiluettavatDays.getValue(keskeytysaika.poissaolonSyy.required()) - hyvaksiluettavatUsed
                            )
                    }
                    hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap[it.key] =
                        max(
                            0.0,
                            hyvaksiluettavatCounterData.hyvaksiluettavatPerYearMap.getValue(it.key) - hyvaksiluettavatUsed
                        )
                    reducedDaysTotal += amountOfReducedDays
                }
                return reducedDaysTotal
            }
        }
    }

    private fun getAmountOfReducedDaysAndHyvaksiluettavatUsed(
        keskeytysaikaLength: Double,
        hyvaksiluettavatLeft: Double
    ): Pair<Double, Double> {
        var amountOfReducedDays = 0.0
        var hyvaksiLuettavatUsed = 0.0
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
                amountOfReducedDays = BigDecimal.valueOf(keskeytysaikaLength)
                    .subtract(BigDecimal.valueOf(hyvaksiluettavatLeft)).toDouble()
                hyvaksiLuettavatUsed = hyvaksiluettavatLeft
            }
            // Keskeytysajan pituus on pienempi kuin jäljellä olevien hyväksiluettavien päivien määrä,
            // vähennetään vain jäljellä olevien hyväksiluettavien päivien määrää.
            else -> {
                hyvaksiLuettavatUsed = keskeytysaikaLength
            }
        }
        return Pair(amountOfReducedDays, hyvaksiLuettavatUsed)
    }

    // Muodostetaan Map, jossa avaimena vuosi ja arvona kuinka monta päivää keskeytysajasta
    // kyseiselle vuodelle sijoittuu.
    private fun getKeskeytysaikaMap(
        startDate: LocalDate,
        endDate: LocalDate,
        keskeytysaikaFactor: Double,
        tyoskentelyjaksoFactor: Double
    ): MutableMap<Int, Double> {
        val keskeytysaikaMap = mutableMapOf<Int, Double>()
        val keskeytysaikaDuringYears = startDate.duringYears(endDate)
        val first = keskeytysaikaDuringYears.first()
        val last = keskeytysaikaDuringYears.last()

        keskeytysaikaDuringYears.forEach {
            val dateFrom =
                if (it == first) startDate
                else it.startOfYearDate()
            val dateTo =
                if (it == last) endDate
                else it.endOfYearDate()
            val betweenDays = dateFrom.daysBetween(dateTo)
            // Kerrotaan myös työskentelyjakson osa-aikaprosentilla, koska esim. 50% poissaolo 50% mittaisesta
            // työpäivästä vähentää hyväksiluettavia päiviä kyseisen päivän osalta vain 0,25 päivää.
            keskeytysaikaMap[it] = betweenDays * keskeytysaikaFactor * tyoskentelyjaksoFactor
        }

        return keskeytysaikaMap
    }

    private fun getEndDate(endDate: LocalDate, calculateUntilDate: LocalDate?): LocalDate {
        return if (calculateUntilDate != null && calculateUntilDate < endDate) calculateUntilDate
        else endDate
    }
}

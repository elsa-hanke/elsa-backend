package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.required

import java.time.Clock
import java.time.LocalDate
import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyyTyyppi.*
import fi.elsapalvelu.elsa.extensions.daysBetween
import fi.elsapalvelu.elsa.extensions.duringYears
import fi.elsapalvelu.elsa.extensions.endOfYearDate
import fi.elsapalvelu.elsa.extensions.startOfYearDate
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksonPituusCounterService
import fi.elsapalvelu.elsa.service.tyoskentely.PoissaolonHyvaksilukulaskuri
import fi.elsapalvelu.elsa.service.constants.HYVAKSILUETTAVAT_DAYS
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.max

@Service
@Transactional
class TyoskentelyjaksonPituusCounterServiceImpl(private val clock: Clock) : TyoskentelyjaksonPituusCounterService {

    override fun calculateInDays(
        tyoskentelyjakso: Tyoskentelyjakso,
        vahennettavatPaivat: Double?
    ): Double {
        val nykyinenPaiva = LocalDate.now(clock)
        val laskennanPaattymispaiva =
            if (tyoskentelyjakso.paattymispaiva == null || tyoskentelyjakso.paattymispaiva.required() > nykyinenPaiva)
                nykyinenPaiva
            else
                tyoskentelyjakso.paattymispaiva.required()
        // Lasketaan työskentelyjakson päivät
        val paivienMaara = tyoskentelyjakso.alkamispaiva.required().daysBetween(laskennanPaattymispaiva)

        // Ei huomioida tulevaisuuden jaksoja
        if (paivienMaara > 0) {
            val tyoskentelyjaksoFactor = tyoskentelyjakso.osaaikaprosentti.required().toDouble() / 100.0
            var result = tyoskentelyjaksoFactor * paivienMaara

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
                LocalDate.now(clock)
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

        when (keskeytysaika.poissaolonSyy.required().vahennystyyppi.required()) {
            VAHENNETAAN_SUORAAN -> {
                return keskeytysaikaLength
            }
            VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI -> {
                val keskeytysaikaMap = getKeskeytysaikaMap(
                    keskeytysaika.alkamispaiva.required(),
                    endDate,
                    keskeytysaikaFactor,
                    tyoskentelyjaksoFactor
                )
                return keskeytysaikaMap.entries.sumOf { (vuosi, poissaolopaivat) ->
                    PoissaolonHyvaksilukulaskuri.laskeKestostaVahennettavatPaivat(
                        poissaolopaivat, vuosi, keskeytysaika.poissaolonSyy.required(), hyvaksiluettavatCounterData
                    )
                }
            }
        }
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

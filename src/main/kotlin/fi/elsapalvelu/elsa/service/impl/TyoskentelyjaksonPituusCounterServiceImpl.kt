package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA
import fi.elsapalvelu.elsa.service.TyoskentelyjaksonPituusCounterService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

@Service
@Transactional
class TyoskentelyjaksonPituusCounterServiceImpl : TyoskentelyjaksonPituusCounterService {

    override fun calculateInDays(tyoskentelyjakso: Tyoskentelyjakso): Double {
        // Lasketaan työskentelyjakson päivät
        val daysBetween = ChronoUnit.DAYS.between(
            tyoskentelyjakso.alkamispaiva,
            tyoskentelyjakso.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
        ) + 1

        // Ei huomioida tulevaisuuden jaksoja
        if (daysBetween > 0) {
            val factor = tyoskentelyjakso.osaaikaprosentti!!.toDouble() / 100.0
            var result = factor * daysBetween

            // Vähennetään keskeytykset
            tyoskentelyjakso.keskeytykset.map { keskeytysaika ->
                val keskeytysaikaDaysBetween = ChronoUnit.DAYS.between(
                    keskeytysaika.alkamispaiva,
                    keskeytysaika.paattymispaiva
                ) + 1

                val keskeytysaikaProsentti = keskeytysaika.osaaikaprosentti!!.toDouble()
                // Keskeytysajan prosentti 0 % tarkoittaa kokopäiväpoissaoloa
                val keskeytysaikaFactor =
                    if (keskeytysaikaProsentti == 0.0) 1.0
                    else keskeytysaikaProsentti / 100.0
                val keskeytysaikaResult = keskeytysaikaFactor * keskeytysaikaDaysBetween

                when (keskeytysaika.poissaolonSyy!!.vahennystyyppi!!) {
                    VAHENNETAAN_YLIMENEVA_AIKA -> {
                        // 30 kalenteripäivän sääntöä ei oteta huomioon vielä. Tarvitaan jokin ratkaisu.
                        result -= keskeytysaikaResult
                    }
                    VAHENNETAAN_SUORAAN -> {
                        result -= keskeytysaikaResult
                    }
                }
            }

            // Koskaan ei summata negatiivisa arvoja laskuriin! (Esim. jos on kirjattu poissaolo useampaan kertaan)
            return max(0.0, result)
        }

        return 0.0
    }
}

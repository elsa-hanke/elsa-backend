package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.tyoskentely.HyvaksiluettavatCounterData
import java.time.LocalDate
import org.springframework.stereotype.Component

@Component
class TyoskentelyajanPaallekkaisyystarkistus(private val pituuslaskuri: TyoskentelyjaksonPituusCounterService) {
    fun onkoTyoskentelyaikaSallittu(
        tyoskentelyjaksot: List<Tyoskentelyjakso>,
        alkamispaiva: LocalDate,
        paattymispaiva: LocalDate,
        uudenJaksonOsuus: Double? = null
    ): Boolean {
        // Tulevaisuudessa alkavaa avointa jaksoa ei tarkisteta ennen sen alkamista.
        if (paattymispaiva < alkamispaiva) return true

        var hyvaksilukusaldo: HyvaksiluettavatCounterData? = null
        for (paiva in alkamispaiva.datesUntil(paattymispaiva.plusDays(1))) {
            val paivanOsuus = PaivittaisenTyoskentelyajanLaskuri.laskeOsuus(tyoskentelyjaksot, paiva, uudenJaksonOsuus) {
                // Sama muuttuva saldo jaetaan kaikkien tämän tarkistuksen päivien ja poissaolojen kesken.
                hyvaksilukusaldo ?: pituuslaskuri.calculateHyvaksiluettavatDaysLeft(tyoskentelyjaksot, paiva.minusDays(1))
                    .also { hyvaksilukusaldo = it }
            }
            if (paivanOsuus > 1.0) return false
        }
        return true
    }
}

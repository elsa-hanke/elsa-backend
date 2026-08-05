package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.HyvaksiluettavatCounterData
import java.time.LocalDate

interface TyoskentelyjaksonPituusCounterService {

    fun calculateInDays(
        tyoskentelyjakso: Tyoskentelyjakso,
        vahennettavatPaivat: Double?
    ): Double

    fun calculateHyvaksiluettavatDaysLeft(
        tyoskentelyjaksot: List<Tyoskentelyjakso>, calculateUntilDate: LocalDate? = null
    ): HyvaksiluettavatCounterData

    fun getHyvaksiluettavatPerYearMap(tyoskentelyjaksot: List<Tyoskentelyjakso>): MutableMap<Int, Double>

    fun calculateAmountOfReducedDaysAndUpdateHyvaksiluettavatCounter(
        keskeytysaika: Keskeytysaika,
        tyoskentelyjaksoFactor: Double,
        hyvaksiluettavatCounterData: HyvaksiluettavatCounterData,
        calculateUntilDate: LocalDate? = null
    ): Double

}

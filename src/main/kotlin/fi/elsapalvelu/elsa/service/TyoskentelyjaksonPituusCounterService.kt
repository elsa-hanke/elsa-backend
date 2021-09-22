package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.HyvaksiluettavatCounterData
import java.time.LocalDate

interface TyoskentelyjaksonPituusCounterService {

    fun calculateInDays(
        tyoskentelyjakso: Tyoskentelyjakso,
        hyvaksiluettavatCounterData: HyvaksiluettavatCounterData
    ): Double

    fun calculateHyvaksiluettavatDaysLeft(
        tyoskentelyjaksot: List<Tyoskentelyjakso>, calculateUntilDate: LocalDate? = null
    ): HyvaksiluettavatCounterData

    fun getHyvaksiluettavatPerYearMap(tyoskentelyjaksot: List<Tyoskentelyjakso>): MutableMap<Int, Double>

}

package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.service.dto.HyvaksiluettavatCounterData

interface TyoskentelyjaksonPituusCounterService {

    fun calculateInDays(tyoskentelyjakso: Tyoskentelyjakso, hyvaksiluettavatCounterData: HyvaksiluettavatCounterData): Double

    fun getHyvaksiluettavatPerYearMap(tyoskentelyjaksot: List<Tyoskentelyjakso>): MutableMap<Int, Double>

}

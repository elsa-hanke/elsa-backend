package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso

interface TyoskentelyjaksonPituusCounterService {

    fun calculateInDays(tyoskentelyjakso: Tyoskentelyjakso): Double

}

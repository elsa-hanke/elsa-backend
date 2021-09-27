package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO

interface OverlappingTyoskentelyjaksoValidationService {

    fun validateTyoskentelyjakso(userId: String, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): Boolean

    fun validateKeskeytysaika(
        userId: String,
        keskeytysaikaDTO: KeskeytysaikaDTO
    ): Boolean

    fun validateKeskeytysaikaDelete(
        userId: String,
        keskeytysaikaId: Long
    ): Boolean

}

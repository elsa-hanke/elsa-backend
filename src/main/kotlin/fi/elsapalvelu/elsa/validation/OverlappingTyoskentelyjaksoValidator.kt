package fi.elsapalvelu.elsa.validation

import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO

interface OverlappingTyoskentelyjaksoValidator {

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

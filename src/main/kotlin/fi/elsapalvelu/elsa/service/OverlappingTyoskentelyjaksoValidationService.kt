package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO

interface OverlappingTyoskentelyjaksoValidationService {

    fun validateTyoskentelyjakso(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): Boolean

    fun validateKeskeytysaika(
        opintooikeusId: Long,
        keskeytysaikaDTO: KeskeytysaikaDTO
    ): Boolean

    fun validateKeskeytysaikaDelete(
        opintooikeusId: Long,
        keskeytysaikaId: Long
    ): Boolean

}

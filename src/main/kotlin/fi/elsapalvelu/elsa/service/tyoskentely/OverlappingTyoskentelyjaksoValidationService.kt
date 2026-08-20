package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO

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

package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO

interface OverlappingKeskeytysaikaValidationService {

    fun validateKeskeytysaika(opintooikeusId: Long, keskeytysaikaDTO: KeskeytysaikaDTO): Boolean

}

package fi.elsapalvelu.elsa.service.tyoskentely

import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO

interface OverlappingKeskeytysaikaValidationService {

    fun validateKeskeytysaika(opintooikeusId: Long, keskeytysaikaDTO: KeskeytysaikaDTO): Boolean

}

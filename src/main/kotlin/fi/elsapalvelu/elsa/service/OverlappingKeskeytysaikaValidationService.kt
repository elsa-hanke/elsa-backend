package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO

interface OverlappingKeskeytysaikaValidationService {

    fun validateKeskeytysaika(userId: String, keskeytysaikaDTO: KeskeytysaikaDTO): Boolean

}

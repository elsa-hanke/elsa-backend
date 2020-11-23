package fi.elsapalvelu.elsa.service.dto

import javax.validation.constraints.NotNull

data class KayttooikeusHakemusDTO(

    @get: NotNull
    var yliopisto: String? = null

)

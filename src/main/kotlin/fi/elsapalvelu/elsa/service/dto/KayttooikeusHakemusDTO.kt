package fi.elsapalvelu.elsa.service.dto

import jakarta.validation.constraints.NotNull

data class KayttooikeusHakemusDTO(

    @get: NotNull
    var yliopisto: Long? = null

)

package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import jakarta.validation.constraints.NotNull

data class OpintosuoritusTyyppiDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: OpintosuoritusTyyppiEnum? = null
)

package fi.elsapalvelu.elsa.service.dto

import javax.validation.constraints.NotNull

data class OpintosuoritusTyyppiDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi_fi: String? = null,

    var nimi_sv: String? = null

)

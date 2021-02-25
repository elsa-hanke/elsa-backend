package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.NotNull

data class ArviointityokaluDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null

) : Serializable

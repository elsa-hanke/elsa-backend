package fi.elsapalvelu.elsa.service.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.io.Serializable

data class SuoritemerkinnanSuoriteDTO(

    @get: Min(value = 1)
    @get: Max(value = 5)
    var arviointiasteikonTaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var vaativuustaso: Int? = null,

    var suoriteId: Long? = null

) : Serializable

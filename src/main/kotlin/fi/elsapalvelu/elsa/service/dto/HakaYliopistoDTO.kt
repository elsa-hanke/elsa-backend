package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class HakaYliopistoDTO(

    @get: NotNull
    var hakaId: String? = null,

    @get: NotNull
    var nimi: String? = null


) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HakaYliopistoDTO) return false
        return hakaId != null && hakaId == other.hakaId
    }

    override fun hashCode() = 31
}

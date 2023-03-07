package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class YliopistoDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is YliopistoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

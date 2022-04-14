package fi.elsapalvelu.elsa.service.dto.sarakesign

import java.io.Serializable

data class SarakeSignResponseDTO(

    var request: SarakeSignResponseRequestDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SarakeSignResponseDTO) return false
        return request != null && request == other.request
    }

    override fun hashCode() = 31
}

package fi.oulu.elsa.service.dto

import java.io.Serializable

/**
 * A DTO for the [fi.oulu.elsa.domain.Yliopisto] entity.
 */
data class YliopistoDTO(

    var id: Long? = null,

    var nimi: String? = null,

    var erikoisalas: MutableSet<ErikoisalaDTO> = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is YliopistoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

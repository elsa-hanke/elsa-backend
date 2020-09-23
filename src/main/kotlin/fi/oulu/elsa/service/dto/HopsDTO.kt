package fi.oulu.elsa.service.dto

import java.io.Serializable

/**
 * A DTO for the [fi.oulu.elsa.domain.Hops] entity.
 */
data class HopsDTO(

    var id: Long? = null,

    var suunnitelmanTunnus: String? = null,

    var erikoistuvaLaakariId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HopsDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

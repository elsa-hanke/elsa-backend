package fi.oulu.elsa.service.dto

import java.io.Serializable

/**
 * A DTO for the [fi.oulu.elsa.domain.Pikaviesti] entity.
 */
data class PikaviestiDTO(

    var id: Long? = null,

    var sisalto: String? = null,

    var keskusteluId: Long? = null,

    var lahettajaId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PikaviestiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

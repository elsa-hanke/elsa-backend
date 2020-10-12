package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.NotNull

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.Erikoisala] entity.
 */
data class ErikoisalaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoisalaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

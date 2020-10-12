package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.Opintooikeustiedot] entity.
 */
data class OpintooikeustiedotDTO(

    var id: Long? = null,

    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloPaattyy: LocalDate? = null,

    var erikoisala: String? = null,

    var yliopistoId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintooikeustiedotDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

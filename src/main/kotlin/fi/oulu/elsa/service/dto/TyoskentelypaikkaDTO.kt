package fi.oulu.elsa.service.dto

import fi.oulu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import java.io.Serializable

/**
 * A DTO for the [fi.oulu.elsa.domain.Tyoskentelypaikka] entity.
 */
data class TyoskentelypaikkaDTO(

    var id: Long? = null,

    var nimi: String? = null,

    var tyyppi: TyoskentelyjaksoTyyppi? = null,

    var tyoskentelyjaksoId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyoskentelypaikkaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

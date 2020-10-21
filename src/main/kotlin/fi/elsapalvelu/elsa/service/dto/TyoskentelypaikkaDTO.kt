package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import java.io.Serializable

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

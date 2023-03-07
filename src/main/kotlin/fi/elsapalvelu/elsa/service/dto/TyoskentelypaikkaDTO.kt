package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class TyoskentelypaikkaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var tyyppi: TyoskentelyjaksoTyyppi? = null,

    var muuTyyppi: String? = null,

    var kuntaId: String? = null,

    var kunta: KuntaDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyoskentelypaikkaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

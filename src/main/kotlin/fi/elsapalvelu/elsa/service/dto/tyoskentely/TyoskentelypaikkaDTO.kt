package fi.elsapalvelu.elsa.service.dto.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.TyoskentelyjaksoTyyppi
import java.io.Serializable
import jakarta.validation.constraints.NotNull

import fi.elsapalvelu.elsa.service.dto.perustiedot.KuntaDTO
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
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyoskentelypaikkaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

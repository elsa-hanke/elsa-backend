package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikonTasoTyyppi
import java.io.Serializable
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class ArviointiasteikonTasoDTO(

    var id: Long? = null,

    @get: NotNull
    @get: Min(value = 1)
    @get: Max(value = 5)
    var taso: Int? = null,

    @get: NotNull
    var nimi: ArviointiasteikonTasoTyyppi? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointiasteikonTasoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

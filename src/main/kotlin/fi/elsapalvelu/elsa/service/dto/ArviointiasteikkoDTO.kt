package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikkoTyyppi
import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class ArviointiasteikkoDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: ArviointiasteikkoTyyppi? = null,

    var tasot: Set<ArviointiasteikonTasoDTO>? = setOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointiasteikkoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

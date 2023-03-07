package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class ErikoisalaWithTehtavatyypitDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var vastuuhenkilonTehtavatyypit: Set<VastuuhenkilonTehtavatyyppiDTO> = setOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoisalaWithTehtavatyypitDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

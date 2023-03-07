package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.util.*
import jakarta.validation.constraints.NotNull

data class IlmoitusDTO(

    var id: Long? = null,

    @get: NotNull
    var teksti: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IlmoitusDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}

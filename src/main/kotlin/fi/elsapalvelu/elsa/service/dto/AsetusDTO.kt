package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.util.*
import javax.validation.constraints.NotNull

data class AsetusDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AsetusDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}

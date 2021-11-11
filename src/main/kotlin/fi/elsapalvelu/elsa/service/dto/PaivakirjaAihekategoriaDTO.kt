package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.util.*
import javax.validation.constraints.NotNull

data class PaivakirjaAihekategoriaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var kuvaus: String? = null,

    var jarjestysnumero: Int? = null,

    @get: NotNull
    var teoriakoulutus: Boolean? = null,

    @get: NotNull
    var muunAiheenNimi: Boolean? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaivakirjaAihekategoriaDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}

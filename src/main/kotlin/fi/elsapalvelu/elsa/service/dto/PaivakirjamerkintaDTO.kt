package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import java.util.*
import javax.persistence.Lob
import javax.validation.constraints.NotNull

data class PaivakirjamerkintaDTO(

    var id: Long? = null,

    @get: NotNull
    var paivamaara: LocalDate? = null,

    @get: NotNull
    var oppimistapahtumanNimi: String? = null,

    var muunAiheenNimi: String? = null,

    @Lob var reflektio: String? = null,

    @get: NotNull
    var yksityinen: Boolean? = null,

    var aihekategoriat: MutableSet<PaivakirjaAihekategoriaDTO> = mutableSetOf(),

    var teoriakoulutus: TeoriakoulutusDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaivakirjamerkintaDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}

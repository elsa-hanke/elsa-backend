package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class OppimistavoitteenKategoriaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var voimassaolonAlkamispaiva: LocalDate? = null,

    var voimassaolonPaattymispaiva: LocalDate? = null,

    var erikoisalaId: Long? = null,

    var arviointiasteikko: ArviointiasteikkoDTO? = null,

    var oppimistavoitteet: MutableSet<OppimistavoiteDTO>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OppimistavoitteenKategoriaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

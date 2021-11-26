package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class OppimistavoiteDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var voimassaolonAlkamispaiva: LocalDate? = null,

    var voimassaolonPaattymispaiva: LocalDate? = null,

    var kategoriaId: Long? = null,

    var vaadittulkm: Int? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OppimistavoiteDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

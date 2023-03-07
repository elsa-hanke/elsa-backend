package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull

data class SuoriteDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    @get: NotNull
    var voimassaolonAlkamispaiva: LocalDate? = null,

    var voimassaolonPaattymispaiva: LocalDate? = null,

    var kategoriaId: Long? = null,

    var vaadittulkm: Int? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoriteDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

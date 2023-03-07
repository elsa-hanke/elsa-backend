package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull

data class SuoriteWithSuoritemerkinnatDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    @get: NotNull
    var voimassaolonAlkamispaiva: LocalDate? = null,

    var voimassaolonPaattymispaiva: LocalDate? = null,

    var vaadittulkm: Int? = null,

    var suoritemerkinnat: List<SuoritemerkintaDTO>? = listOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoriteWithSuoritemerkinnatDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull

data class SuoritteenKategoriaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    var erikoisalaId: Long? = null,

    var suoritteet: Set<SuoriteDTO>? = null,

    var jarjestysnumero: Int? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritteenKategoriaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

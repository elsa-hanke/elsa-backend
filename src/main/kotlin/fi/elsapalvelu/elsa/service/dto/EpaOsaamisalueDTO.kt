package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class EpaOsaamisalueDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var kuvaus: String? = null,

    @get: NotNull
    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloLoppuu: LocalDate? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EpaOsaamisalueDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

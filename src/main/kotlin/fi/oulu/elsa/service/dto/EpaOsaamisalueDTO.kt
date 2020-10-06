package fi.oulu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

/**
 * A DTO for the [fi.oulu.elsa.domain.EpaOsaamisalue] entity.
 */
data class EpaOsaamisalueDTO(

    var id: Long? = null,

    @get: NotNull
    var epaTunnus: String? = null,

    @get: NotNull
    var epaNimi: String? = null,

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

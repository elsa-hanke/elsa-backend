package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.OsaamisenArviointi] entity.
 */
data class OsaamisenArviointiDTO(

    var id: Long? = null,

    var tunnus: String? = null,

    var osasto: String? = null,

    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

    var tyoskentelyjaksoId: Long? = null,

    var arvioitavaId: Long? = null,

    var arvioijaId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OsaamisenArviointiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.Tyoskentelyjakso] entity.
 */
data class TyoskentelyjaksoDTO(

    var id: Long? = null,

    @get: NotNull
    var tunnus: String? = null,

    var osasto: String? = null,

    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

    var erikoistuvaLaakariId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyoskentelyjaksoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

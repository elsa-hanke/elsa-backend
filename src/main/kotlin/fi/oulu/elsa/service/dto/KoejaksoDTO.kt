package fi.oulu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

/**
 * A DTO for the [fi.oulu.elsa.domain.Koejakso] entity.
 */
data class KoejaksoDTO(

    var id: Long? = null,

    var ohjeteksti: String? = null,

    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

    var erikoistuvaLaakariId: Long? = null,

    var lahikouluttajaId: Long? = null,

    var vastuuhenkiloId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KoejaksoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

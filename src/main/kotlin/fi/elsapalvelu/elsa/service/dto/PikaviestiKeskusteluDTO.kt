package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.PikaviestiKeskustelu] entity.
 */
data class PikaviestiKeskusteluDTO(

    var id: Long? = null,

    var aihe: String? = null,

    var keskustelijas: MutableSet<KayttajaDTO> = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PikaviestiKeskusteluDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

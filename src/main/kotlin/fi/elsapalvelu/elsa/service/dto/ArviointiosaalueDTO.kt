package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.Arviointiosaalue] entity.
 */
data class ArviointiosaalueDTO(

    var id: Long? = null,

    var aluetunnus: String? = null,

    var nimi: String? = null,

    var kuvaus: String? = null,

    var osaamisenRajaarvo: String? = null,

    var minimivaatimus: String? = null,

    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloLoppuu: LocalDate? = null,

    var osaamisalueenArviointiId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointiosaalueDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

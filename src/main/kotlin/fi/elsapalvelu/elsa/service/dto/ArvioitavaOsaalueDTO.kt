package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.CanmedsRooli
import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue] entity.
 */
data class ArvioitavaOsaalueDTO(

    var id: Long? = null,

    @get: NotNull
    var tunnus: String? = null,

    @get: NotNull
    var nimi: String? = null,

    var kuvaus: String? = null,

    var osaamisenRajaarvo: String? = null,

    var arviointikriteerit: String? = null,

    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloLoppuu: LocalDate? = null,

    @get: NotNull
    var rooli: CanmedsRooli? = null,

    var epaOsaamisalueId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaOsaalueDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

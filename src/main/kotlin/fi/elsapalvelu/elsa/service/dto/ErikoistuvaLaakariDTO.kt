package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

/**
 * A DTO for the [fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari] entity.
 */
data class ErikoistuvaLaakariDTO(

    var id: Long? = null,

    @get: NotNull
    var puhelinnumero: String? = null,

    @get: NotNull
    var sahkoposti: String? = null,

    var opiskelijatunnus: String? = null,

    @get: Min(value = 1900)
    @get: Max(value = 2100)
    var opintojenAloitusvuosi: Int? = null,

    var kayttajaId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoistuvaLaakariDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

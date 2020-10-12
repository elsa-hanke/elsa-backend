package fi.oulu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.Max
import javax.validation.constraints.Min

/**
 * A DTO for the [fi.oulu.elsa.domain.OsaalueenArviointi] entity.
 */
data class OsaalueenArviointiDTO(

    var id: Long? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var arvio: Int? = null,

    var arvioitavaOsaalueId: Long? = null,

    var suoritusarviointiId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OsaalueenArviointiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class KouluttajavaltuutusDTO(

    var id: Long? = null,

    @get: NotNull
    var alkamispaiva: LocalDate? = null,

    @get: NotNull
    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    var valtuutuksenLuontiaika: Instant? = null,

    @get: NotNull
    var valtuutuksenMuokkausaika: Instant? = null,

    var valtuuttajaId: Long? = null,

    var valtuutettuId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KouluttajavaltuutusDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

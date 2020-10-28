package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class TyoskentelyjaksoDTO(

    var id: Long? = null,

    @get: NotNull
    var tunnus: String? = null,

    @get: NotNull
    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    @get: Min(value = 50)
    @get: Max(value = 100)
    var osaaikaprosentti: Int? = null,

    var tyoskentelypaikkaId: Long? = null,

    var erikoistuvaLaakariId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TyoskentelyjaksoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class KeskeytysaikaDTO(

    var id: Long? = null,

    @get: NotNull
    var alkamispaiva: LocalDate? = null,

    @get: NotNull
    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    @get: Min(value = 0)
    @get: Max(value = 100)
    var poissaoloprosentti: Int? = null,

    var poissaolonSyyId: Long? = null,

    var tyoskentelyjaksoId: Long? = null,

    var poissaolonSyy: PoissaolonSyyDTO? = null,

    var tyoskentelyjakso: TyoskentelyjaksoDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeskeytysaikaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

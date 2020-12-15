package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi
import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class PoissaolonSyyDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var vahennystyyppi: PoissaolonSyyTyyppi? = null,

    @get: NotNull
    var voimassaolonAlkamispaiva: LocalDate? = null,

    var voimassaolonPaattymispaiva: LocalDate? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PoissaolonSyyDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

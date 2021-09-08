package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.Max
import javax.validation.constraints.Min

data class ErikoistuvaLaakariDTO(

    var id: Long? = null,

    var opiskelijatunnus: String? = null,

    var syntymaaika: LocalDate? = null,

    var erikoistumisenAloituspaiva: LocalDate? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var opintooikeudenPaattymispaiva: LocalDate? = null,

    @get: Min(value = 1900)
    @get: Max(value = 2100)
    var opintojenAloitusvuosi: Int? = null,

    var kayttajaId: Long? = null,

    var erikoisalaId: Long? = null,

    var erikoisalaNimi: String? = null,

    var yliopisto: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoistuvaLaakariDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.Max
import javax.validation.constraints.Min

data class SuoritusarviointiDTO(

    var id: Long? = null,

    var tapahtumanAjankohta: LocalDate? = null,

    var arvioitavaTapahtuma: String? = null,

    var pyynnonAika: LocalDate? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var vaativuustaso: Int? = null,

    var sanallinenArvio: String? = null,

    var arviointiAika: LocalDate? = null,

    var arvioitavaId: Long? = null,

    var arvioijaId: Long? = null,

    var arvioitavaOsaalueId: Long? = null,

    var tyoskentelyjaksoId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.Max
import javax.validation.constraints.Min

data class SeurantajaksonArviointiDTO(

    var arvioitavaTapahtuma: String? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var arviointiasteikonTaso: Int? = null,

    var tapahtumanAjankohta: LocalDate? = null,

    var arviointiasteikko: ArviointiasteikkoDTO? = null,

    var suoritusarviointiId: Long? = null

) : Serializable {
    override fun toString() = "SeurantajaksonArviointiDTO"
}

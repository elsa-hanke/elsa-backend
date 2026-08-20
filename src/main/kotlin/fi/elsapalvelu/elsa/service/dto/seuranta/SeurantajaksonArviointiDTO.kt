package fi.elsapalvelu.elsa.service.dto.seuranta

import java.time.LocalDate
import java.io.Serializable
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointiasteikkoDTO
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

    companion object {
        private const val serialVersionUID = 1L
    }
}

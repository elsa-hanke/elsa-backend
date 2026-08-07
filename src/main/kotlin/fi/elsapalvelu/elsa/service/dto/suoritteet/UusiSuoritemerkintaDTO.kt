package fi.elsapalvelu.elsa.service.dto.suoritteet

import java.time.LocalDate
import jakarta.validation.constraints.NotNull
import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointiasteikkoDTO
data class UusiSuoritemerkintaDTO(

    @get: NotNull
    var suorituspaiva: LocalDate? = null,

    var lisatiedot: String? = null,

    var tyoskentelyjaksoId: Long? = null,

    var suoritteet: List<SuoritemerkinnanSuoriteDTO>? = listOf(),

    var arviointiasteikko: ArviointiasteikkoDTO? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

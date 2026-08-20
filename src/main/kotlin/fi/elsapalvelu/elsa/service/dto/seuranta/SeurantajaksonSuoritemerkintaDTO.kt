package fi.elsapalvelu.elsa.service.dto.seuranta

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritemerkintaDTO
data class SeurantajaksonSuoritemerkintaDTO(

    var suorite: String? = null,

    var suoritemerkinnat: List<SuoritemerkintaDTO>? = null

) : Serializable {
    override fun toString() = "SeurantajaksonSuoriteMerkintaDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

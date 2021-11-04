package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SeurantajaksonSuoritemerkintaDTO(

    var oppimistavoite: String? = null,

    var suoritemerkinnat: List<SuoritemerkintaDTO>? = null

) : Serializable {
    override fun toString() = "SeurantajaksonSuoriteMerkintaDTO"
}

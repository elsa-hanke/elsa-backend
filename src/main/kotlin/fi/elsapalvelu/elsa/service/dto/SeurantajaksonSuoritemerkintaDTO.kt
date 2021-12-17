package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SeurantajaksonSuoritemerkintaDTO(

    var suorite: String? = null,

    var suoritemerkinnat: List<SuoritemerkintaDTO>? = null

) : Serializable {
    override fun toString() = "SeurantajaksonSuoriteMerkintaDTO"
}

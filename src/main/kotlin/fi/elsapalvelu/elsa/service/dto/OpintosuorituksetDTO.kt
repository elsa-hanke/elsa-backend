package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class OpintosuorituksetDTO(

    var opintosuoritukset: List<OpintosuoritusDTO>? = null,

    var johtamisopinnotSuoritettu: Double? = null,

    var johtamisopinnotVaadittu: Double? = null,

    var sateilysuojakoulutuksetSuoritettu: Double? = null,

    var sateilysuojakoulutuksetVaadittu: Double? = null

): Serializable {
    override fun toString() = "OpintosuorituksetDTO"
}


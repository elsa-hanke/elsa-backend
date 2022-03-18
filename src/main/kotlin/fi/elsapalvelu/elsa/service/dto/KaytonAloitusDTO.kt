package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KaytonAloitusDTO (

    var sahkoposti: String? = null,

    var opintooikeusId: Long? = null

) : Serializable {
    override fun toString() = "KaytonAloitusDTO"
}


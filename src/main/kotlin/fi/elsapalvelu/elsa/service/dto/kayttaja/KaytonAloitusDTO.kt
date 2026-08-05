package fi.elsapalvelu.elsa.service.dto.kayttaja

import java.io.Serializable

data class KaytonAloitusDTO (

    var sahkoposti: String? = null,

    var opintooikeusId: Long? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "KaytonAloitusDTO"
}

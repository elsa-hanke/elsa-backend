package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ValmistumispyyntoHyvaksyntaFormDTO(

    var korjausehdotus: String? = null,

    var sahkoposti: String? = null,

    var puhelinnumero: String? = null

) : Serializable {
    override fun toString() = "ValmistumispyyntoHyvaksyntaFormDTO"
}

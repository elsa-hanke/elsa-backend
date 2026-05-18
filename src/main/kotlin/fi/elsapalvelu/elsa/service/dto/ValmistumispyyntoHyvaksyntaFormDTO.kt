package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ValmistumispyyntoHyvaksyntaFormDTO(

    var korjausehdotus: String? = null,

    var sahkoposti: String? = null,

    var puhelinnumero: String? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "ValmistumispyyntoHyvaksyntaFormDTO"
}

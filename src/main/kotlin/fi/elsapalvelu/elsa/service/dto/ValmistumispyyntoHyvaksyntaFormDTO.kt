package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ValmistumispyyntoHyvaksyntaFormDTO(

    var korjausehdotus: String? = null

) : Serializable {
    override fun toString() = "ValmistumispyyntoHyvaksyntaFormDTO"
}

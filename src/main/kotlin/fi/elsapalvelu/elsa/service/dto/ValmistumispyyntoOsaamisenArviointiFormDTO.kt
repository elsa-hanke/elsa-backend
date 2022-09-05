package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ValmistumispyyntoOsaamisenArviointiFormDTO(

    var osaaminenRiittavaValmistumiseen: Boolean? = null,

    var korjausehdotus: String? = null

): Serializable {
    override fun toString() = "ValmistumispyyntoOsaamisenArviointiFormDTO"
}

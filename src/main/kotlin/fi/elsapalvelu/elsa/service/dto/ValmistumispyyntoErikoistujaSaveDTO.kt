package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ValmistumispyyntoErikoistujaSaveDTO (

    var selvitysVanhentuneistaSuorituksista: String? = null

) : Serializable {
    override fun toString() = "ValmistumispyyntoErikoistujaSaveDTO"
}

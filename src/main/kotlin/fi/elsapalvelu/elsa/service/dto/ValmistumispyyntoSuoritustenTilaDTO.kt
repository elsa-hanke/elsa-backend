package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import java.io.Serializable

data class ValmistumispyyntoSuoritustenTilaDTO(

    var erikoisalaTyyppi: ErikoisalaTyyppi? = null,

    var vanhojaTyoskentelyjaksojaOrSuorituksiaExists: Boolean? = null,

    var kuulusteluVanhentunut: Boolean? = null

) : Serializable {
    override fun toString() = "ValmistumispyyntoSuoritustenTilaDTO"
}


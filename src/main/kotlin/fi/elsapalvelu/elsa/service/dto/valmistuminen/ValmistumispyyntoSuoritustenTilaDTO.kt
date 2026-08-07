package fi.elsapalvelu.elsa.service.dto.valmistuminen

import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import java.io.Serializable

data class ValmistumispyyntoSuoritustenTilaDTO(

    var erikoisalaTyyppi: ErikoisalaTyyppi? = null,

    var vanhojaTyoskentelyjaksojaOrSuorituksiaExists: Boolean? = null,

    var kuulusteluVanhentunut: Boolean? = null

) : Serializable {
    override fun toString() = "ValmistumispyyntoSuoritustenTilaDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}


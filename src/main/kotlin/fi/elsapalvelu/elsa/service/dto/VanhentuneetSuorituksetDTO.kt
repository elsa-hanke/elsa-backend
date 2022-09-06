package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class VanhentuneetSuorituksetDTO (

    var vanhojaTyoskentelyjaksojaOrSuorituksiaExists: Boolean? = null,

    var kuulusteluVanhentunut: Boolean? = null

) : Serializable {
    override fun toString() = "VanhentuneetSuorituksetDTO"
}

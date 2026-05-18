package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksotTilastotTyoskentelyjaksotDTO(

    var id: Long,

    var suoritettu: Double

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun toString() = "TyoskentelyjaksotTilastotTyoskentelyjaksotDTO"
}

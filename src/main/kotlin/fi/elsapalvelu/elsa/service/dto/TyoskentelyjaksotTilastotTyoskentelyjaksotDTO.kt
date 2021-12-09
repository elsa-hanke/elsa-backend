package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksotTilastotTyoskentelyjaksotDTO(

    var id: Long,

    var suoritettu: Double

) : Serializable {
    override fun toString() = "TyoskentelyjaksotTilastotTyoskentelyjaksotDTO"
}

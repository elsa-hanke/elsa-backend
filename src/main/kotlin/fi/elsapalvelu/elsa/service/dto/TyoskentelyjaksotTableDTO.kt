package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksotTableDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var keskeytykset: MutableSet<KeskeytysaikaDTO> = mutableSetOf(),

    var tilastot: TyoskentelyjaksotTilastotDTO? = null,

    var terveyskeskuskoulutusjaksoLahetetty: Boolean? = null

) : Serializable {
    override fun toString() = "TyoskentelyjaksotTableDTO"
}

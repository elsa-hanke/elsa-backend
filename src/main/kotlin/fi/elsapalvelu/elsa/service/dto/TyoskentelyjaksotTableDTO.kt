package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksotTableDTO(

    var poissaolonSyyt: MutableSet<PoissaolonSyyDTO> = mutableSetOf(),

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var keskeytykset: MutableSet<KeskeytysaikaDTO> = mutableSetOf(),

    var tilastot: TyoskentelyjaksotTilastotDTO? = null

) : Serializable

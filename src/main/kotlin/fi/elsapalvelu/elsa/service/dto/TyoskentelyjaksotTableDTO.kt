package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksotTableDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var keskeytykset: MutableSet<KeskeytysaikaDTO> = mutableSetOf()

) : Serializable

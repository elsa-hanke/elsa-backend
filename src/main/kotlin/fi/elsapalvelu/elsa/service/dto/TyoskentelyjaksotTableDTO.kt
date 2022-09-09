package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import java.io.Serializable

class TyoskentelyjaksotTableDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var keskeytykset: MutableSet<KeskeytysaikaDTO> = mutableSetOf(),

    var tilastot: TyoskentelyjaksotTilastotDTO? = null,

    var terveyskeskuskoulutusjaksonTila: TerveyskeskuskoulutusjaksoTila? = null,

    var terveyskeskuskoulutusjaksonKorjausehdotus: String? = null

) : Serializable {
    override fun toString() = "TyoskentelyjaksotTableDTO"
}

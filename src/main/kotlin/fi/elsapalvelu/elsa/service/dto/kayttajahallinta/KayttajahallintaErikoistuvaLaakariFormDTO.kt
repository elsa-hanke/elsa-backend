package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.service.dto.*
import java.io.Serializable

class KayttajahallintaErikoistuvaLaakariFormDTO(

    var yliopistot: MutableSet<YliopistoDTO> = mutableSetOf(),

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf(),

    var asetukset: MutableSet<AsetusDTO> = mutableSetOf(),

    var opintooppaat: MutableSet<OpintoopasSimpleDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "KayttajahallintaErikoistuvaLaakariFormDTO"
}

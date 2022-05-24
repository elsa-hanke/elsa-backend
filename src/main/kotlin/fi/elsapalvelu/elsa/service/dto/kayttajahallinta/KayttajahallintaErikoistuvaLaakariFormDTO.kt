package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.service.dto.AsetusDTO
import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.OpintoopasDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import java.io.Serializable

class KayttajahallintaErikoistuvaLaakariFormDTO(

    var yliopistot: MutableSet<YliopistoDTO> = mutableSetOf(),

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf(),

    var asetukset: MutableSet<AsetusDTO> = mutableSetOf(),

    var opintooppaat: MutableSet<OpintoopasDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "KayttajahallintaErikoistuvaLaakariFormDTO"
}

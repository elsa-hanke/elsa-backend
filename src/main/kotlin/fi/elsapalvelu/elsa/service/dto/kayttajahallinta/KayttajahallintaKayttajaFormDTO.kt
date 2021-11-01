package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import java.io.Serializable

class KayttajahallintaKayttajaFormDTO(

    var yliopistot: MutableSet<YliopistoDTO> = mutableSetOf(),

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "KayttajaFormDTO"
}
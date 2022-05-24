package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.service.dto.ErikoisalaWithTehtavatyypitDTO
import fi.elsapalvelu.elsa.service.dto.KayttajahallintaFormVastuuhenkiloDTO
import java.io.Serializable

class KayttajahallintaVastuuhenkilonTehtavatFormDTO(

    var erikoisalat: Set<ErikoisalaWithTehtavatyypitDTO> = setOf(),

    var vastuuhenkilot: Set<KayttajahallintaFormVastuuhenkiloDTO> = setOf()

): Serializable {
    override fun toString() = "KayttajahallintaVastuuhenkilonTehtavatFormDTO"
}

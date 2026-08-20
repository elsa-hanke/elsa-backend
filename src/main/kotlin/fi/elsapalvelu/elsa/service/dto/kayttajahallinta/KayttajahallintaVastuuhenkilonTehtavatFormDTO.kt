package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaWithTehtavatyypitDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajahallintaFormVastuuhenkiloDTO
import java.io.Serializable

class KayttajahallintaVastuuhenkilonTehtavatFormDTO(

    var erikoisalat: Set<ErikoisalaWithTehtavatyypitDTO> = setOf(),

    var vastuuhenkilot: Set<KayttajahallintaFormVastuuhenkiloDTO> = setOf()

): Serializable {
    override fun toString() = "KayttajahallintaVastuuhenkilonTehtavatFormDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.elsapalvelu.elsa.service.dto.koejakso

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.YliopistoDTO
data class KoulutussopimusFormDTO(

    var vastuuhenkilo: KayttajaDTO? = null,

    var yliopistot: List<YliopistoDTO>? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

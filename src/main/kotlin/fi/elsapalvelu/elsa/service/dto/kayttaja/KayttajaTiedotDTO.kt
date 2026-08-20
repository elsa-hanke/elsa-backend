package fi.elsapalvelu.elsa.service.dto.kayttaja

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.YliopistoDTO
data class KayttajaTiedotDTO(

    var nimike: String? = null,

    var kayttajanYliopistot: MutableSet<YliopistoDTO>? = mutableSetOf(),

    var kayttajanYliopistotJaErikoisalat: MutableSet<KayttajaYliopistoErikoisalatDTO>? = mutableSetOf(),

    var yliopistot: List<YliopistoDTO> = listOf(),

    var erikoisalat: List<ErikoisalaDTO> = listOf()

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

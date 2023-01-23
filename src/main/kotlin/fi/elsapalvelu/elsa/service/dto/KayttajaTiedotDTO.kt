package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KayttajaTiedotDTO(

    var nimike: String? = null,

    var kayttajanYliopistot: MutableSet<KayttajaYliopistoErikoisalatDTO>? = mutableSetOf(),

    var yliopistot: List<YliopistoDTO> = listOf(),

    var erikoisalat: List<ErikoisalaDTO> = listOf()

) : Serializable

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KayttajaErikoisalatPerYliopistoDTO(

    var yliopistoNimi: String? = null,

    var erikoisalat: List<String>? = null

) : Serializable

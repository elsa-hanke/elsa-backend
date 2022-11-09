package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class PalauteDTO(

    var palautteenAihe: String? = null,

    var palaute: String? = null,

    var anonyymiPalaute: Boolean = false

): Serializable

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KoulutussopimusFormDTO (

    var vastuuhenkilot: List<KayttajaDTO>? = null,

    var yliopistot: List<YliopistoDTO>? = null,

) : Serializable

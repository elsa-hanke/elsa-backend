package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class VastuuhenkilonArvioFormDTO (

    var vastuuhenkilot: List<KayttajaDTO>? = null,

    var tyoskentelyjaksoLiitetty: Boolean = false,

    var tyoskentelyjaksonPituusRiittava: Boolean = false,

    var tyotodistusLiitetty: Boolean = false

) : Serializable

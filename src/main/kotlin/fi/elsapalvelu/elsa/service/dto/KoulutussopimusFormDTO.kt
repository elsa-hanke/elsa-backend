package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KoulutussopimusFormDTO(

    var vastuuhenkilo: KayttajaDTO? = null,

    var yliopistot: List<YliopistoDTO>? = null,

    ) : Serializable {
    override fun toString() = "KoulutussopimusFormDTO"
}

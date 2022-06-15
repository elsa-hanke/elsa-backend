package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import java.io.Serializable

data class KayttajahallintaYliopistoErikoisalaDTO(

    var yliopisto: YliopistoEnum? = null,

    var erikoisala: String? = null,

    var vastuuhenkilonTehtavat: Set<VastuuhenkilonTehtavatyyppiDTO>? = setOf()

) : Serializable {
    override fun toString() = "YliopistoErikoisalaDTO"
}


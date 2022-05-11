package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import java.io.Serializable

data class YliopistoErikoisalaDTO(

    var yliopisto: YliopistoEnum? = null,

    var erikoisala: String? = null

) : Serializable {
    override fun toString() = "YliopistoErikoisalaDTO"
}


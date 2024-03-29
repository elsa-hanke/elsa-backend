package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import java.io.Serializable

data class OpintosuorituksetPersistenceDTO(

    var yliopisto: YliopistoEnum,

    var items: List<OpintosuoritusDTO>? = null

) : Serializable {
    override fun toString() = "OpintosuorituksetPersistenceDTO"
}

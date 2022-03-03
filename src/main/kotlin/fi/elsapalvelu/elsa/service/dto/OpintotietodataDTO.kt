package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import java.io.Serializable

data class OpintotietodataDTO(

    var yliopisto: YliopistoEnum,

    var opiskelijatunnus: String? = null,

    var syntymaaika: String? = null,

    var opintooikeudet: List<OpintotietoOpintooikeusDataDTO>? = null

) : Serializable {
    override fun toString() = "OpintotietodataDTO"
}

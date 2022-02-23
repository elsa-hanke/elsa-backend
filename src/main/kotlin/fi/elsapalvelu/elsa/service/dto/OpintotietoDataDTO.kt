package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.Yliopisto
import java.io.Serializable

data class OpintotietoDataDTO(

    var yliopisto: Yliopisto? = null,

    var opiskelijatunnus: String? = null,

    var syntymaaika: String? = null,

    var opintooikeudet: List<OpintotietoOpintooikeusDataDTO>? = null

) : Serializable {
    override fun toString() = "OpintotietoDataDTO"
}

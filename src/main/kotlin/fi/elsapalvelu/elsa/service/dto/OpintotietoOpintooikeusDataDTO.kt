package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import java.io.Serializable
import java.time.LocalDate

data class OpintotietoOpintooikeusDataDTO(

    var id: String? = null,

    var opintooikeudenAlkamispaiva: LocalDate?,

    var opintooikeudenPaattymispaiva: LocalDate?,

    var asetus: String? = null,

    var erikoisalaTunniste: String? = null,

    var tila: OpintooikeudenTila? = null,

    var yliopisto: YliopistoEnum,

    var opiskelijatunnus: String? = null,

    ) : Serializable {
    override fun toString() = "OpintotietoOpintooikeusDataDTO"
}

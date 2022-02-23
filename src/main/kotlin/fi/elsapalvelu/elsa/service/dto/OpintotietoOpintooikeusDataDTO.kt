package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import java.io.Serializable
import java.time.LocalDate

data class OpintotietoOpintooikeusDataDTO(

    var id: String? = null,

    var opintooikeudenAlkamispaiva: LocalDate?,

    var opintooikeudenPaattymispaiva: LocalDate?,

    var asetus: String? = null,

    var tutkintoohjelmaId: String? = null,

    var tila: OpintooikeudenTila? = null

) : Serializable {
    override fun toString() = "OpintotietoOpintooikeusDataDTO"
}

package fi.elsapalvelu.elsa.service.dto.koulutus

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import java.io.Serializable

data class OpintotietoOpintooikeusDataDTO(

    var id: String? = null,

    var opintooikeudenAlkamispaiva: LocalDate?,

    var opintooikeudenPaattymispaiva: LocalDate?,

    var asetus: String? = null,

    var erikoisalaTunnisteList: List<String>? = listOf(),

    var tila: OpintooikeudenTila? = null,

    var yliopisto: YliopistoEnum,

    var opiskelijatunnus: String? = null,

    ) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "OpintotietoOpintooikeusDataDTO"
}

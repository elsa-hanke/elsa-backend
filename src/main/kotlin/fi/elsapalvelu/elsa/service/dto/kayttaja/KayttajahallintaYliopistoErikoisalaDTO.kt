package fi.elsapalvelu.elsa.service.dto.kayttaja

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.perustiedot.VastuuhenkilonTehtavatyyppiDTO
data class KayttajahallintaYliopistoErikoisalaDTO(

    var yliopisto: YliopistoEnum? = null,

    var erikoisala: String? = null,

    var vastuuhenkilonTehtavat: Set<VastuuhenkilonTehtavatyyppiDTO>? = setOf()

) : Serializable {
    override fun toString() = "YliopistoErikoisalaDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}


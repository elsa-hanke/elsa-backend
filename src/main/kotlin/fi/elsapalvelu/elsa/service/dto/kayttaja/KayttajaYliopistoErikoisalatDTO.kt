package fi.elsapalvelu.elsa.service.dto.kayttaja

import java.io.Serializable
import jakarta.validation.constraints.NotNull

import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.YliopistoDTO
data class KayttajaYliopistoErikoisalatDTO(

    @get: NotNull
    var yliopisto: YliopistoDTO? = null,

    @get: NotNull
    var erikoisalat: List<ErikoisalaDTO>? = null,

    ) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

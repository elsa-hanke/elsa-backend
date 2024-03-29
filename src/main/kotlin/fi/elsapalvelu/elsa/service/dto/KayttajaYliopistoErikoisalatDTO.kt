package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class KayttajaYliopistoErikoisalatDTO(

    @get: NotNull
    var yliopisto: YliopistoDTO? = null,

    @get: NotNull
    var erikoisalat: List<ErikoisalaDTO>? = null,

    ) : Serializable

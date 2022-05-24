package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.ReassignedVastuuhenkilonTehtavaTyyppi
import java.io.Serializable
import javax.validation.constraints.NotNull

data class ReassignedVastuuhenkilonTehtavaDTO(

    @NotNull
    var kayttajaYliopistoErikoisala: KayttajaYliopistoErikoisalaDTO? = null,

    @NotNull
    var tehtavaId: Long? = null,

    @NotNull
    var tyyppi: ReassignedVastuuhenkilonTehtavaTyyppi? = null

): Serializable {
    override fun toString() = "ReassignedVastuuhenkilonTehtavaDTO()"
}

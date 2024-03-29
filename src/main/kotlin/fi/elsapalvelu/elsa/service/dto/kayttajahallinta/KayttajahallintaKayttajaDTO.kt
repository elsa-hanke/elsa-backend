package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import fi.elsapalvelu.elsa.service.dto.KayttajaYliopistoErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.ReassignedVastuuhenkilonTehtavaDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import java.io.Serializable
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty

data class KayttajahallintaKayttajaDTO(

    var etunimi: String? = null,

    var sukunimi: String? = null,

    @get: Email
    @get: NotEmpty
    var sahkoposti: String,

    var yliopisto: YliopistoDTO? = null,

    var eppn: String? = null,

    var yliopistotAndErikoisalat: Set<KayttajaYliopistoErikoisalaDTO> = setOf(),

    var reassignedTehtavat: Set<ReassignedVastuuhenkilonTehtavaDTO> = setOf()

) : Serializable {
    override fun toString() = "KayttajahallintaKayttajaDTO"
}


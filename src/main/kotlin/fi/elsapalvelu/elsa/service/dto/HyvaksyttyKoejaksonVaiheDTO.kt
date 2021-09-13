package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import java.io.Serializable
import java.time.LocalDate

data class HyvaksyttyKoejaksonVaiheDTO(

    var id: Long? = null,

    var tyyppi: KoejaksoTyyppi? = null,

    var pvm: LocalDate? = null

) : Serializable {
    override fun toString() = "HyvaksyttyKoejaksonVaiheDTO"
}

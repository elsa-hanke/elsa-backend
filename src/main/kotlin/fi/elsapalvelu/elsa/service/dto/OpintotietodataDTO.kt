package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class OpintotietodataDTO(

    var syntymaaika: LocalDate? = null,

    var opintooikeudet: List<OpintotietoOpintooikeusDataDTO>? = null

) : Serializable {
    override fun toString() = "OpintotietodataDTO"
}

package fi.elsapalvelu.elsa.service.dto.koulutus

import java.io.Serializable

data class OpintotietodataDTO(

    var syntymaaika: LocalDate? = null,

    var opintooikeudet: List<OpintotietoOpintooikeusDataDTO>? = null

) : Serializable {
    override fun toString() = "OpintotietodataDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

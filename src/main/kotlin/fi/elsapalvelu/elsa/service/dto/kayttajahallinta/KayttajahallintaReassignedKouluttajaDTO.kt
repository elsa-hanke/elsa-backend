package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.io.Serializable

data class KayttajahallintaReassignedKouluttajaDTO(

    var reassignedKayttajaId: Long? = null

) : Serializable {
    override fun toString() = "KayttajahallintaReassignedKouluttajaDTO"
}


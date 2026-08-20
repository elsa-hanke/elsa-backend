package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.io.Serializable

data class KayttajahallintaReassignedKouluttajaDTO(

    var reassignedKayttajaId: Long? = null

) : Serializable {
    override fun toString() = "KayttajahallintaReassignedKouluttajaDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}


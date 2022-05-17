package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.io.Serializable

data class KayttajahallintaUpdateKayttajaDTO(

    var sahkoposti: String? = null

) : Serializable {
    override fun toString() = "KayttajahallintaUpdateKayttajaDTO"
}


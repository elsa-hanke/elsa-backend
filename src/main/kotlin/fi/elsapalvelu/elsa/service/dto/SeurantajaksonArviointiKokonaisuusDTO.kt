package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SeurantajaksonArviointiKokonaisuusDTO(

    var nimi: String? = null,

    var arvioinnit: List<SuoritusarviointiDTO>? = null

) : Serializable {
    override fun toString() = "SeurantajaksonArviointiKokonaisuusDTO"
}

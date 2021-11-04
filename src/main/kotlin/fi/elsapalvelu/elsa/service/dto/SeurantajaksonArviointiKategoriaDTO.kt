package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SeurantajaksonArviointiKategoriaDTO(

    var nimi: String? = null,

    var jarjestysnumero: Int? = null,

    var arvioitavatKokonaisuudet: List<SeurantajaksonArviointiKokonaisuusDTO>? = null

) : Serializable {
    override fun toString() = "SeurantajaksonArviointiKategoriaDTO"
}

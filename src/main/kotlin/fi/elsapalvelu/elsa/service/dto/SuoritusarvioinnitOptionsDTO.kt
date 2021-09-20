package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SuoritusarvioinnitOptionsDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var arvioitavatKokonaisuudet: MutableSet<ArvioitavaKokonaisuusDTO> = mutableSetOf(),

    var tapahtumat: MutableSet<SuoritusarviointiDTO> = mutableSetOf(),

    var kouluttajatAndVastuuhenkilot: MutableSet<KayttajaDTO> = mutableSetOf()

) : Serializable {
    override fun toString(): String {
        return "SuoritusarvioinnitOptionsDTO()"
    }
}

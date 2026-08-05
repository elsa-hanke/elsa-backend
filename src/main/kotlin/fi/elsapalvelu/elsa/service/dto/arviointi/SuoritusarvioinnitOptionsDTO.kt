package fi.elsapalvelu.elsa.service.dto.arviointi

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
data class SuoritusarvioinnitOptionsDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var arvioitavatKokonaisuudet: MutableSet<ArvioitavaKokonaisuusDTO> = mutableSetOf(),

    var tapahtumat: MutableSet<SuoritusarviointiDTO> = mutableSetOf(),

    var kouluttajatAndVastuuhenkilot: MutableSet<KayttajaDTO> = mutableSetOf()

) : Serializable {
    override fun toString(): String {
        return "SuoritusarvioinnitOptionsDTO()"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

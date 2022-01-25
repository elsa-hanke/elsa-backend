package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ArviointipyyntoFormDTO(

    var tyoskentelyjaksot: Set<TyoskentelyjaksoDTO> = setOf(),

    var kunnat: Set<KuntaDTO> = setOf(),

    var erikoisalat: Set<ErikoisalaDTO> = setOf(),

    var arvioitavanKokonaisuudenKategoriat: Set<ArvioitavanKokonaisuudenKategoriaDTO> = setOf(),

    var kouluttajatAndVastuuhenkilot: Set<KayttajaDTO> = setOf()

) : Serializable {
    override fun toString() = "ArviointipyyntoFormDTO"
}


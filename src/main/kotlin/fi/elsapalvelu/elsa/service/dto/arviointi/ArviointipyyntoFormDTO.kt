package fi.elsapalvelu.elsa.service.dto.arviointi

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.KuntaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
data class ArviointipyyntoFormDTO(

    var tyoskentelyjaksot: Set<TyoskentelyjaksoDTO> = setOf(),

    var kunnat: Set<KuntaDTO> = setOf(),

    var erikoisalat: Set<ErikoisalaDTO> = setOf(),

    var arvioitavanKokonaisuudenKategoriat: Set<ArvioitavanKokonaisuudenKategoriaDTO> = setOf(),

    var kouluttajatAndVastuuhenkilot: Set<KayttajaDTO> = setOf()

) : Serializable {
    override fun toString() = "ArviointipyyntoFormDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}


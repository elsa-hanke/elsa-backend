package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class SuoritemerkintaFormDTO(

    var tyoskentelyjaksot: Set<TyoskentelyjaksoDTO> = setOf(),

    var kunnat: Set<KuntaDTO> = setOf(),

    var erikoisalat: Set<ErikoisalaDTO> = setOf(),

    var suoritteenKategoriat: Set<SuoritteenKategoriaDTO> = setOf()

) : Serializable {
    override fun toString() = "SuoritemerkintaFormDTO"
}

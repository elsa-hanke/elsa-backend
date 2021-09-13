package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class SuoritemerkintaFormDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var kunnat: MutableSet<KuntaDTO> = mutableSetOf(),

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf(),

    var oppimistavoitteenKategoriat: MutableSet<OppimistavoitteenKategoriaDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "SuoritemerkintaFormDTO"
}

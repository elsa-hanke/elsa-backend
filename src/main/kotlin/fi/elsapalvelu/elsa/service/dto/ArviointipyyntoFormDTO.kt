package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ArviointipyyntoFormDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var kunnat: MutableSet<KuntaDTO> = mutableSetOf(),

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf(),

    var epaOsaamisalueet: MutableSet<EpaOsaamisalueDTO> = mutableSetOf(),

    var kouluttajat: MutableSet<KayttajaDTO> = mutableSetOf()

) : Serializable

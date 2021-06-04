package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksoFormDTO(

    var kunnat: MutableSet<KuntaDTO> = mutableSetOf(),

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf(),

    var reservedAsiakirjaNimet: MutableSet<String> = mutableSetOf()

) : Serializable

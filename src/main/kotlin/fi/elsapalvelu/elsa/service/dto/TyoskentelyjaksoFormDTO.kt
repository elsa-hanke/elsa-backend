package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksoFormDTO(

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf(),

) : Serializable

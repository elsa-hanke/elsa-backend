package fi.elsapalvelu.elsa.service.dto.tyoskentely

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.perustiedot.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.KuntaDTO
class TyoskentelyjaksoFormDTO(

    var kunnat: MutableSet<KuntaDTO> = mutableSetOf(),

    var erikoisalat: MutableSet<ErikoisalaDTO> = mutableSetOf(),

    var reservedAsiakirjaNimet: MutableSet<String> = mutableSetOf()

) : Serializable {
    override fun toString() = "TyoskentelyjaksoFormDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

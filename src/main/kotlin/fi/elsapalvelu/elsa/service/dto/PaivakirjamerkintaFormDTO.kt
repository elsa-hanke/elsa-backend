package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class PaivakirjamerkintaFormDTO(

    var aihekategoriat: MutableSet<PaivakirjaAihekategoriaDTO> = mutableSetOf(),

    var teoriakoulutukset: MutableSet<TeoriakoulutusDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "PaivakirjamerkintaFormDTO"
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class PaivakirjamerkinnatOptionsDTO(

    var aihekategoriat: MutableSet<PaivakirjaAihekategoriaDTO> = mutableSetOf()

) : Serializable {
    override fun toString(): String {
        return "PaivakirjamerkinnatOptionsDTO()"
    }
}


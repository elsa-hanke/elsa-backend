package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class PaivakirjamerkinnatOptionsDTO(

    var aihekategoriat: MutableSet<PaivakirjaAihekategoriaDTO> = mutableSetOf()

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString(): String {
        return "PaivakirjamerkinnatOptionsDTO()"
    }
}


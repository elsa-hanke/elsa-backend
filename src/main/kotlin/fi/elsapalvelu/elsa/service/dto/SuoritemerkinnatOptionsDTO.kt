package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SuoritemerkinnatOptionsDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var suoritteet: MutableSet<SuoriteDTO> = mutableSetOf()

) : Serializable {
    override fun toString(): String {
        return "SuoritemerkinnatOptionsDTO()"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

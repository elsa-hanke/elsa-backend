package fi.elsapalvelu.elsa.service.dto.suoritteet

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
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

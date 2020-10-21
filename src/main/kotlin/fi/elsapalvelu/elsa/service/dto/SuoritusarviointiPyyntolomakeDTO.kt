package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SuoritusarviointiPyyntolomakeDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var kouluttajat: MutableSet<KayttajaDTO> = mutableSetOf()
) : Serializable {
    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ArviointipyyntoFormDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var epaOsaamisalueet: MutableSet<EpaOsaamisalueDTO> = mutableSetOf(),

    var kouluttajat: MutableSet<KayttajaDTO> = mutableSetOf()

) : Serializable {
    override fun hashCode() = 31
}

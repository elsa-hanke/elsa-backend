package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SuoritusarvioinnitDto(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var osaamisalueet: MutableSet<EpaOsaamisalueDTO> = mutableSetOf(),

    var tapahtumat: MutableSet<SuoritusarviointiDTO> = mutableSetOf(),

    var kouluttajat: MutableSet<KayttajaDTO> = mutableSetOf()

) : Serializable {
    override fun hashCode() = 31
}

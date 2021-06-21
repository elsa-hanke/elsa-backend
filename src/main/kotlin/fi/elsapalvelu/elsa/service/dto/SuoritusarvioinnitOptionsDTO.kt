package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SuoritusarvioinnitOptionsDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var epaOsaamisalueet: MutableSet<EpaOsaamisalueDTO> = mutableSetOf(),

    var tapahtumat: MutableSet<SuoritusarviointiDTO> = mutableSetOf(),

    var kouluttajatAndVastuuhenkilot: MutableSet<KayttajaDTO> = mutableSetOf()

) : Serializable {
    override fun hashCode() = 31
}

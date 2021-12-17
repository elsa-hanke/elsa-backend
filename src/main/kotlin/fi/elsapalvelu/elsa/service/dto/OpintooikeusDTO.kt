package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class OpintooikeusDTO(

    var id: Long? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var opintooikeudenPaattymispaiva: LocalDate? = null,

    var opiskelijatunnus: String? = null,

    var asetus: String? = null,

    var osaamisenArvioinninOppaanPvm: LocalDate? = null,

    var yliopistoNimi: String? = null,

    var erikoisalaId: Long? = null,

    var erikoisalaNimi: String? = null,

    var opintoopasId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintooikeusDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

}

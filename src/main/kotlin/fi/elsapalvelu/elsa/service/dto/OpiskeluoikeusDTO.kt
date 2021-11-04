package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class OpiskeluoikeusDTO(

    var id: Long? = null,

    var opintosuunnitelmaKaytossaPvm: LocalDate? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var opintooikeudenPaattymispaiva: LocalDate? = null,

    var opiskelijatunnus: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpiskeluoikeusDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

}

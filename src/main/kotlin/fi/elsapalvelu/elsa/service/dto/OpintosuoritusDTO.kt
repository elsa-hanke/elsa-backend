package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class OpintosuoritusDTO(

    var id: Long? = null,

    var nimi_fi: String? = null,

    var nimi_sv: String? = null,

    var kurssikoodi: String? = null,

    var tyyppi: OpintosuoritusTyyppiDTO? = null,

    var suorituspaiva: LocalDate? = null,

    var opintopisteet: Double? = null,

    var hyvaksytty: Boolean? = null,

    var arvio_fi: String? = null,

    var arvio_sv: String? = null,

    var vanhenemispaiva: LocalDate? = null,

    var yliopistoOpintooikeusId: String? = null,

    var osakokonaisuudet: List<OpintosuoritusOsakokonaisuusDTO>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

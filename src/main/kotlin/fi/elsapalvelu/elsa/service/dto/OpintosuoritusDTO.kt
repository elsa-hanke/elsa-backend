package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class OpintosuoritusDTO(

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
    override fun toString() = "OpintosuoritusDTO{" +
        ", nimi_fi='$nimi_fi'" +
        ", nimi_sv='$nimi_sv'" +
        ", kurssikoodi='$kurssikoodi'" +
        ", suorituspaiva='$suorituspaiva'" +
        ", opintopisteet='$opintopisteet'" +
        ", hyvaksytty='$hyvaksytty'" +
        ", arvio_fi='$arvio_fi'" +
        ", arvio_sv='${arvio_sv}v'" +
        ", vanhenemispaiva='$vanhenemispaiva'" +
        ", yliopistoOpintooikeusId='$yliopistoOpintooikeusId'" +
        "}"
}

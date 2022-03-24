package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class OpintosuoritusOsakokonaisuusDTO(

    var id: Long? = null,

    var nimi_fi: String? = null,

    var nimi_sv: String? = null,

    var kurssikoodi: String? = null,

    @get: NotNull
    var suorituspaiva: LocalDate,

    var opintopisteet: Double? = null,

    var hyvaksytty: Boolean? = null,

    var arvio_fi: String? = null,

    var arvio_sv: String? = null,

    var vanhenemispaiva: LocalDate? = null

) : Serializable {
    override fun toString() = "OpintosuoritusDTO{" +
        "id=$id" +
        ", nimi_fi='$nimi_fi'" +
        ", nimi_sv='$nimi_sv'" +
        ", kurssikoodi='$kurssikoodi'" +
        ", suorituspaiva='$suorituspaiva'" +
        ", opintopisteet='$opintopisteet'" +
        ", hyvaksytty='$hyvaksytty'" +
        ", arvio_fi='$arvio_fi'" +
        ", arvio_sv='${arvio_sv}v'" +
        ", vanhenemispaiva='$vanhenemispaiva'" +
        "}"
}



package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class KoulutussopimuksenKouluttajaDTO(

    var id: Long? = null,

    var kayttajaId: Long? = null,

    var nimi: String? = null,

    var nimike: String? = null,

    var toimipaikka: String? = null,

    var lahiosoite: String? = null,

    var postitoimipaikka: String? = null,

    var puhelin: String? = null,

    var sahkoposti: String? = null,

    var sopimusHyvaksytty: Boolean? = null,

    var kuittausaika: LocalDate? = null

) : Serializable

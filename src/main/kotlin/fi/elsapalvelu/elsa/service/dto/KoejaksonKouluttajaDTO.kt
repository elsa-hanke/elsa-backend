package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class KoejaksonKouluttajaDTO(

    var id: Long? = null,

    var kayttajaUserId: String? = null,

    var nimi: String? = null,

    var sopimusHyvaksytty: Boolean? = null,

    var kuittausaika: LocalDate? = null

) : Serializable

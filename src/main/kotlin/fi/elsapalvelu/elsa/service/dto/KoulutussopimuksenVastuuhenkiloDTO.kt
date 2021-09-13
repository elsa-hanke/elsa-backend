package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class KoulutussopimuksenVastuuhenkiloDTO(

    var id: Long? = null,

    var nimi: String? = null,

    var nimike: String? = null,

    var sopimusHyvaksytty: Boolean? = null,

    var kuittausaika: LocalDate? = null

) : Serializable {
    override fun toString() = "KoulutussopimuksenVastuuhenkiloDTO"
}

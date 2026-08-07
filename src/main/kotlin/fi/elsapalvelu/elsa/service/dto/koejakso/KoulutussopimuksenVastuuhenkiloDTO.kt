package fi.elsapalvelu.elsa.service.dto.koejakso

import java.time.LocalDate
import java.io.Serializable

data class KoulutussopimuksenVastuuhenkiloDTO(

    var id: Long? = null,

    var nimi: String? = null,

    var nimike: String? = null,

    var puhelin: String? = null,

    var sahkoposti: String? = null,

    var sopimusHyvaksytty: Boolean? = null,

    var kuittausaika: LocalDate? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "KoulutussopimuksenVastuuhenkiloDTO"
}

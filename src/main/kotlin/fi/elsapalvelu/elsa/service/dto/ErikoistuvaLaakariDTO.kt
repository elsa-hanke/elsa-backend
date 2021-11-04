package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class ErikoistuvaLaakariDTO(

    var id: Long? = null,

    var nimi: String? = null,

    @get: NotNull
    var sahkoposti: String? = null,

    var syntymaaika: LocalDate? = null,

    var kayttajaId: Long? = null,

    var yliopisto: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoistuvaLaakariDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

}

package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.SeurantajaksoTila
import java.io.Serializable
import java.time.LocalDate

data class EtusivuSeurantajaksoDTO(

    var id: Long? = null,

    var erikoistujanNimi: String? = null,

    var tallennettu: LocalDate? = null,

    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

    var tila: SeurantajaksoTila? = null

) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EtusivuSeurantajaksoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

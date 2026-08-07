package fi.elsapalvelu.elsa.service.dto.seuranta

import java.time.LocalDate
import fi.elsapalvelu.elsa.service.dto.enumeration.SeurantajaksoTila
import java.io.Serializable

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

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class EtusivuArviointipyyntoDTO(

    var id: Long? = null,

    var erikoistujanNimi: String? = null,

    var pyynnonAika: LocalDate? = null

): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EtusivuArviointipyyntoDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

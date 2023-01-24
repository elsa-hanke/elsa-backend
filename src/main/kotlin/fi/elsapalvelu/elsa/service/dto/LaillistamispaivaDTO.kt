package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Lob

data class LaillistamispaivaDTO(

    var laillistamispaiva: LocalDate? = null,

    @Lob
    var laillistamistodistus: ByteArray? = null,

    var laillistamistodistusNimi: String? = null,

    var laillistamistodistusTyyppi: String? = null

) : Serializable {


    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LaillistamispaivaDTO

        if (laillistamispaiva != other.laillistamispaiva) return false
        if (laillistamistodistus != null) {
            if (other.laillistamistodistus == null) return false
            if (!laillistamistodistus.contentEquals(other.laillistamistodistus)) return false
        } else if (other.laillistamistodistus != null) return false

        return true
    }

    override fun toString() = "LaillistamispaivaDTO"
}

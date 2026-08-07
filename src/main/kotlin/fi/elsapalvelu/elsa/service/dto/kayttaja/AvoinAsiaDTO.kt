package fi.elsapalvelu.elsa.service.dto.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.AvoinAsiaTyyppiEnum
import java.io.Serializable

data class AvoinAsiaDTO(

    var id: Long? = null,

    var tyyppi: AvoinAsiaTyyppiEnum? = null,

    var asia: String? = null,

    var pvm: LocalDate? = null

    ): Serializable {

    override fun hashCode() = 31

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AvoinAsiaDTO

        if (id != other.id) return false
        if (tyyppi != other.tyyppi) return false
        if (asia != other.asia) return false
        if (pvm != other.pvm) return false

        return true
    }
}

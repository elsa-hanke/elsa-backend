package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.AvoinAsiaTyyppiEnum
import java.io.Serializable
import java.time.LocalDate

data class AvoinAsiaDTO(

    var id: Long? = null,

    var tyyppi: AvoinAsiaTyyppiEnum? = null,

    var asia: String? = null,

    var pvm: LocalDate? = null

    ): Serializable {

    override fun hashCode() = 31

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

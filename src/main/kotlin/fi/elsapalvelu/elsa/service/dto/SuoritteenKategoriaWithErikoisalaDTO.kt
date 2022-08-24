package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.NotNull

data class SuoritteenKategoriaWithErikoisalaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    var jarjestysnumero: Int? = null,

    @get: NotNull
    var erikoisala: ErikoisalaDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritteenKategoriaWithErikoisalaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}
package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO(

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
        if (other !is ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

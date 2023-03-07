package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikkoTyyppi
import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class SuoritteenKategoriaWithSuoritemerkinnatDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    var arviointiasteikko: ArviointiasteikkoTyyppi? = null,

    var suoritteet: List<SuoriteWithSuoritemerkinnatDTO>? = null,

    var jarjestysnumero: Int? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritteenKategoriaWithSuoritemerkinnatDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

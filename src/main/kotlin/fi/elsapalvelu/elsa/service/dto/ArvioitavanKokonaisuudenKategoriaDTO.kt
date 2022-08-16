package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class ArvioitavanKokonaisuudenKategoriaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    var jarjestysnumero: Int? = null,

    var arvioitavatKokonaisuudet: Set<ArvioitavaKokonaisuusDTO>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavanKokonaisuudenKategoriaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArvioitavanKokonaisuudenKategoriaDTO"
}

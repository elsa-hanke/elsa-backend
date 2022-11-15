package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.NotNull

data class ArvioitavanKokonaisuudenKategoriaWithArvioinnitDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    var jarjestysnumero: Int? = null,

    var arviointejaYhteensa: Int? = null,

    var arvioitavatKokonaisuudet: List<ArvioitavaKokonaisuusWithArviointiDTO>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavanKokonaisuudenKategoriaWithArvioinnitDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArvioitavanKokonaisuudenKategoriaDTO"
}

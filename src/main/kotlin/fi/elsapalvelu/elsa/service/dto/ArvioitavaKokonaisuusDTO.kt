package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class ArvioitavaKokonaisuusDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var kuvaus: String? = null,

    @get: NotNull
    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloLoppuu: LocalDate? = null,

    var erikoisalaId: Long? = null,

    var kategoria: ArvioitavanKokonaisuudenKategoriaSimpleDTO? = null,

    var arviointiasteikko: ArviointiasteikkoDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaKokonaisuusDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

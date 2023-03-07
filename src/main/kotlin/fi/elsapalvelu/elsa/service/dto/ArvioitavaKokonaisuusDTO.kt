package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull

data class ArvioitavaKokonaisuusDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    var kuvaus: String? = null,

    var kuvausSv: String? = null,

    @get: NotNull
    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloLoppuu: LocalDate? = null,

    var erikoisalaId: Long? = null,

    @get: NotNull
    var kategoria: ArvioitavanKokonaisuudenKategoriaSimpleDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaKokonaisuusDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArvioitavaKokonaisuusDTO(id=$id)"
}

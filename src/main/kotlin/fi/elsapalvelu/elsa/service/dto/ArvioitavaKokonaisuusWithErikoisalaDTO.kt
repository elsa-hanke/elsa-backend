package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull

data class ArvioitavaKokonaisuusWithErikoisalaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var nimiSv: String? = null,

    var kuvaus: String? = null,

    var kuvausSv: String? = null,

    @get: NotNull
    var voimassaoloAlkaa: LocalDate? = null,

    var voimassaoloLoppuu: LocalDate? = null,

    @get: NotNull
    var kategoria: ArvioitavanKokonaisuudenKategoriaWithErikoisalaDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaKokonaisuusWithErikoisalaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArvioitavaKokonaisuusWithErikoisala{id=$id, nimi=$nimi}"
}

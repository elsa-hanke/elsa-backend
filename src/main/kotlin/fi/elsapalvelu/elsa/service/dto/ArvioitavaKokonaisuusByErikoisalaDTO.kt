package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ArvioitavaKokonaisuusByErikoisalaDTO(

    var id: Long? = null,

    var nimi: String? = null,

    var voimassaolevat: List<ArvioitavaKokonaisuusDTO>? = null,

    var vanhentuneet: List<ArvioitavaKokonaisuusDTO>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaKokonaisuusByErikoisalaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

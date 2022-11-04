package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class ArvioitavaKokonaisuusByErikoisalaDTO(

    var erikoisalaId: Long? = null,

    var erikoisalaNimi: String? = null,

    var voimassaolevat: List<ArvioitavaKokonaisuusDTO>? = null,

    var vanhentuneet: List<ArvioitavaKokonaisuusDTO>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaKokonaisuusByErikoisalaDTO) return false
        return erikoisalaId != null && erikoisalaId == other.erikoisalaId
    }

    override fun hashCode() = 31
}

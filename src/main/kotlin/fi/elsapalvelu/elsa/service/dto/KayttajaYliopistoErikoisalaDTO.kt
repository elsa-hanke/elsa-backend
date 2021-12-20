package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KayttajaYliopistoErikoisalaDTO (

    var id: Long? = null,

    var kayttajaId: Long? = null,

    var yliopistoNimi: String? = null,

    var erikoisalaNimi: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KayttajaYliopistoErikoisalaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "KayttajaYliopistoErikoisalaDTO{id=$id}"
}

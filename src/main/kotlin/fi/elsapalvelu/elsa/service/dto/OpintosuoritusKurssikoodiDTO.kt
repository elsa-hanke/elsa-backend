package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class OpintosuoritusKurssikoodiDTO(

    var id: Long? = null,

    var tunniste: String? = null,

    var tyyppi: OpintosuoritusTyyppiDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintosuoritusKurssikoodiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

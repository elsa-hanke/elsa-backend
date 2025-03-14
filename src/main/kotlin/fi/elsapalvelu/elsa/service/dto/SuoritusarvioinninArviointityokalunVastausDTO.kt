package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SuoritusarvioinninArviointityokalunVastausDTO(

    var id: Long? = null,

    var suoritusarviointiId: Long? = null,

    var arviointityokaluId: Long? = null,

    var arviointityokaluKysymysId: Long? = null,

    var tekstiVastaus: String? = null,

    var valittuVaihtoehtoId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarvioinninArviointityokalunVastausDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.ArviointityokaluKysymysTyyppi
import java.io.Serializable
import java.util.*

data class ArviointityokaluKysymysDTO(

    var id: Long? = null,

    var otsikko: String? = null,

    var pakollinen: Boolean? = null,

    var tyyppi: ArviointityokaluKysymysTyyppi,

    var vaihtoehdot: List<ArviointityokaluKysymysVaihtoehtoDTO>? = null,

    var jarjestysnumero: Int? = 99,

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointityokaluKysymysDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}

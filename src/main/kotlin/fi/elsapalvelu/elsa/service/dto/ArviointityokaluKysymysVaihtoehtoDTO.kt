package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.ArviointityokaluKysymysVaihtoehtoTyyppi
import java.io.Serializable
import java.util.*

data class ArviointityokaluKysymysVaihtoehtoDTO(

    var id: Long? = null,

    var teksti: String? = null,

    var valittu: Boolean? = null,

    var tyyppi: ArviointityokaluKysymysVaihtoehtoTyyppi,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointityokaluKysymysVaihtoehtoDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}

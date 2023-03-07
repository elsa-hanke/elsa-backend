package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.Instant

data class SuoritusarvioinninKommenttiDTO(

    var id: Long? = null,

    var teksti: String? = null,

    var luontiaika: Instant? = null,

    var muokkausaika: Instant? = null,

    var kommentoija: KayttajaDTO? = null,

    var suoritusarviointiId: Long? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarvioinninKommenttiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.Authority
import java.io.Serializable
import javax.persistence.Lob
import javax.validation.constraints.NotNull

data class KayttajaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    @Lob
    var profiilikuva: ByteArray? = null,
    var profiilikuvaContentType: String? = null,

    var userId: String? = null,

    var authorities: MutableSet<Authority>? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KayttajaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "KayttajaDTO{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", profiilikuva='?'" +
        ", profiilikuvaContentType='$profiilikuvaContentType'" +
        "}"
}

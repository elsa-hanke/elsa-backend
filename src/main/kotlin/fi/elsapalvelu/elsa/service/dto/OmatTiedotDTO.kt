package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.persistence.Lob
import javax.validation.constraints.Email
import javax.validation.constraints.Size

data class OmatTiedotDTO(

    @field:Email
    @field:Size(min = 5, max = 254)
    var email: String? = null,

    @field:Size(max = 254)
    var phoneNumber: String? = null,

    @Lob
    var avatar: ByteArray? = null,

    var avatarContentType: String? = null,

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KayttajaDTO) return false

        return false
    }

    override fun hashCode() = 31

    override fun toString() = "OmatTiedotDTO{" +
        "email=$email" +
        ", phoneNumber='$phoneNumber'" +
        ", avatar=?" +
        ", avatarContentType='$avatarContentType'" +
        "}"
}

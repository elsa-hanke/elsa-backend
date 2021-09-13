package fi.elsapalvelu.elsa.service.dto

import org.springframework.web.multipart.MultipartFile
import java.io.Serializable

data class OmatTiedotDTO(

    var email: String,

    var phoneNumber: String? = null,

    var avatar: MultipartFile? = null,

    var avatarUpdated: Boolean = false

) : Serializable {
    override fun toString() = "OmatTiedotDTO"
}

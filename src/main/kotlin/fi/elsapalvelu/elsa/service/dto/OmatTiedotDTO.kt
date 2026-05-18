package fi.elsapalvelu.elsa.service.dto

import org.springframework.web.multipart.MultipartFile
import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class OmatTiedotDTO(

    @get: NotNull
    var email: String? = null,

    var phoneNumber: String? = null,

    var avatar: MultipartFile? = null,

    var avatarUpdated: Boolean = false,

    var nimike: String? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "OmatTiedotDTO"
}

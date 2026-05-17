package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class KatseluoikeusDTO(

    var erikoistujanNimi: String? = null,

    var katseluoikeusVanhenee: LocalDate? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "KatseluoikeusDTO"
}

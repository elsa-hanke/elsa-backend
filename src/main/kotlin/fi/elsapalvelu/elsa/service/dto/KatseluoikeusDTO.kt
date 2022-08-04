package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class KatseluoikeusDTO(

    var erikoistujanNimi: String? = null,

    var katseluoikeusVanhenee: LocalDate? = null

) : Serializable {
    override fun toString() = "KatseluoikeusDTO"
}

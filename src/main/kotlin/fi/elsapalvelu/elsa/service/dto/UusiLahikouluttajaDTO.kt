package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.validation.constraints.NotNull

data class UusiLahikouluttajaDTO(
    @get: NotNull
    var nimi: String? = null,

    @get: NotNull
    var sahkoposti: String? = null

) : Serializable {
    override fun hashCode() = 31
}

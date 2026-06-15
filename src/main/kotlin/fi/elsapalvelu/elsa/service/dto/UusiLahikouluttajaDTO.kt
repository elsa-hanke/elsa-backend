package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import jakarta.validation.constraints.NotNull

data class UusiLahikouluttajaDTO(
    @get: NotNull
    var etunimi: String? = null,

    @get: NotNull
    var sukunimi: String? = null,

    @get: NotNull
    var sahkoposti: String? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

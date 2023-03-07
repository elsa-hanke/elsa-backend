package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.io.Serializable
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty

data class KayttajahallintaErikoistuvaLaakariUpdateDTO(

    @get: NotEmpty
    @get: Email
    var sahkoposti: String

) : Serializable {
    override fun toString() = "KayttajahallintaErikoistuvaLaakariUpdateDTO"
}

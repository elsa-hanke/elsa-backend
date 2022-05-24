package fi.elsapalvelu.elsa.service.dto.kayttajahallinta

import java.io.Serializable
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

data class KayttajahallintaErikoistuvaLaakariUpdateDTO(

    @get: NotEmpty
    @get: Email
    var sahkoposti: String

) : Serializable {
    override fun toString() = "KayttajahallintaErikoistuvaLaakariUpdateDTO"
}

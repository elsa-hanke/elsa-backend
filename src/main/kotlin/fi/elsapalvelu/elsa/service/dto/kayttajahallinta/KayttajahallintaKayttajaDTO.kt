package fi.elsapalvelu.elsa.service.dto.kayttajahallinta


import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import java.io.Serializable

data class KayttajahallintaKayttajaDTO(

    var user: UserDTO? = null,

    var kayttaja: KayttajaDTO? = null,

    var erikoistuvaLaakari: ErikoistuvaLaakariDTO? = null

) : Serializable {
    override fun toString() = "KayttajaDTO"
}

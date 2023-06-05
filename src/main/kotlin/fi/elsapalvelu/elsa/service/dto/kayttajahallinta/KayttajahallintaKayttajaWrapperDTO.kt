package fi.elsapalvelu.elsa.service.dto.kayttajahallinta


import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import java.io.Serializable

data class KayttajahallintaKayttajaWrapperDTO(

    var kayttaja: KayttajaDTO? = null,

    var erikoistuvaLaakari: ErikoistuvaLaakariDTO? = null,

    var avoimiaTehtavia: Boolean? = null

) : Serializable {
    override fun toString() = "KayttajahallintaKayttajaWrapperDTO"
}

package fi.elsapalvelu.elsa.service.dto.koejakso

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.OpintooikeusDTO
data class VastuuhenkilonArvioFormDTO(

    var vastuuhenkilo: KayttajaDTO? = null,

    var tyoskentelyjaksoLiitetty: Boolean = false,

    var tyoskentelyjaksonPituusRiittava: Boolean = false,

    var tyotodistusLiitetty: Boolean = false,

    var muutOpintooikeudet: List<OpintooikeusDTO>? = null,

    var koulutussopimusHyvaksytty: Boolean? = null,

    var virkailijanYhteenveto: String? = null

) : Serializable {
    override fun toString() = "VastuuhenkilonArvioFormDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

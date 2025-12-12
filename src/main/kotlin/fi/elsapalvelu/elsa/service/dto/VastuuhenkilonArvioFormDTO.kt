package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

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
}

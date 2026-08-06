package fi.elsapalvelu.elsa.service.dto.kayttaja

import java.io.Serializable

data class KayttajaErikoisalatPerYliopistoDTO(

    var yliopistoNimi: String? = null,

    var erikoisalat: List<String>? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

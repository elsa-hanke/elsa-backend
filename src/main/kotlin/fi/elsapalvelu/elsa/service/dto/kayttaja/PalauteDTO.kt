package fi.elsapalvelu.elsa.service.dto.kayttaja

import java.io.Serializable

data class PalauteDTO(

    var palautteenAihe: String? = null,

    var palauteYliopisto: String? = null,

    var palaute: String? = null,

    var anonyymiPalaute: Boolean = false

): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

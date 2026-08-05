package fi.elsapalvelu.elsa.service.dto.kayttaja

import java.io.Serializable

data class VastuuhenkilonVastuualueetDTO(

    var terveyskeskuskoulutusjakso: Boolean = false,

    var yekTerveyskeskuskoulutusjakso: Boolean = false,

    var valmistuminen: Boolean = false,

    var yekValmistuminen: Boolean = false

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

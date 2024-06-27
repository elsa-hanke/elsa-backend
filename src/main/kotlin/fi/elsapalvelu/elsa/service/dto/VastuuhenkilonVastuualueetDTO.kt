package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class VastuuhenkilonVastuualueetDTO(

    var terveyskeskuskoulutusjakso: Boolean = false,

    var yekTerveyskeskuskoulutusjakso: Boolean = false,

    var valmistuminen: Boolean = false,

    var yekValmistuminen: Boolean = false

) : Serializable

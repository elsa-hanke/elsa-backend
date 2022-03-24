package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KoulutussopimuksenKoulutuspaikkaDTO(

    var id: Long? = null,

    var nimi: String? = null,

    var koulutussopimusOmanYliopistonKanssa: Boolean? = null,

    var yliopistoId: Long? = null

) : Serializable {
    override fun toString() = "KoulutussopimuksenKoulutuspaikkaDTO"
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KoulutussopimuksenKoulutuspaikkaDTO(

    var id: Long? = null,

    var nimi: String? = null,

    var erikoisala: String? = null,

    var yliopisto: String? = null

) : Serializable

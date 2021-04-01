package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoulutussopimusTila
import java.io.Serializable

data class KoejaksoDTO(

    var koulutussopimus: KoejaksonKoulutussopimusDTO? = null,

    var koulutusSopimuksenTila: KoulutussopimusTila? = null

) : Serializable

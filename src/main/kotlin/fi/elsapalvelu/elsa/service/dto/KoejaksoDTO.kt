package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import java.io.Serializable

data class KoejaksoDTO(

    var koulutussopimus: KoejaksonKoulutussopimusDTO? = null,

    var koulutusSopimuksenTila: KoejaksoTila? = null,

    var aloituskeskustelu: KoejaksonAloituskeskusteluDTO? = null,

    var aloituskeskustelunTila: KoejaksoTila? = null

) : Serializable

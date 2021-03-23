package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoulutussopimusTila
import java.io.Serializable
import java.time.LocalDate

data class KoejaksoDTO(

    var id: Long? = null,

    var alkamispaiva: LocalDate? = null,

    var paattymispaiva: LocalDate? = null,

    var koulutussopimus: KoejaksonKoulutussopimusDTO? = null,

    var koulutusSopimuksenTila: KoulutussopimusTila? = null,

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf()

) : Serializable

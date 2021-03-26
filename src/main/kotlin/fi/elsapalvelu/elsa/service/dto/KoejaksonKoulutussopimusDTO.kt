package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoulutussopimusTila
import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class KoejaksonKoulutussopimusDTO(

    var id: Long? = null,

    @get: NotNull
    var erikoistuvanNimi: String? = null,

    @get: NotNull
    var erikoistuvanOpiskelijatunnus: String? = null,

    @get: NotNull
    var erikoistuvanSyntymaaika: LocalDate? = null,

    @get: NotNull
    var erikoistuvanYliopisto: String? = null,

    @get: NotNull
    var erikoistuvanPuhelinnumero: String? = null,

    @get: NotNull
    var erikoistuvanSahkoposti: String? = null,

    @get: NotNull
    var opintooikeudenMyontamispaiva: LocalDate? = null,

    @get: NotNull
    var koejaksonAlkamispaiva: LocalDate? = null,

    @get: NotNull
    var lahetetty: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    @get: NotNull
    var vastuuhenkilo: KoulutussopimuksenVastuuhenkiloDTO? = null,

    var korjausehdotus: String? = null,

    var kouluttajat: MutableSet<KoulutussopimuksenKouluttajaDTO>? = mutableSetOf(),

    var koulutuspaikat: MutableSet<KoulutussopimuksenKoulutuspaikkaDTO>? = mutableSetOf()

) : Serializable

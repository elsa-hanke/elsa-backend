package fi.elsapalvelu.elsa.service.dto

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

    var erikoistuvanPuhelinnumero: String? = null,

    var erikoistuvanSahkoposti: String? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var koejaksonAlkamispaiva: LocalDate? = null,

    var lahetetty: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var vastuuhenkilo: KoulutussopimuksenVastuuhenkiloDTO? = null,

    var korjausehdotus: String? = null,

    var kouluttajat: MutableSet<KoulutussopimuksenKouluttajaDTO>? = mutableSetOf(),

    var koulutuspaikat: MutableSet<KoulutussopimuksenKoulutuspaikkaDTO>? = mutableSetOf()

) : Serializable

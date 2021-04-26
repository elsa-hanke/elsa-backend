package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class KoejaksonVastuuhenkilonArvioDTO(

    var id: Long? = null,

    @get: NotNull
    var erikoistuvanNimi: String? = null,

    @get: NotNull
    var erikoistuvanErikoisala: String? = null,

    @get: NotNull
    var erikoistuvanOpiskelijatunnus: String? = null,

    @get: NotNull
    var erikoistuvanYliopisto: String? = null,

    @get: NotNull
    var vastuuhenkilo: KoejaksonKouluttajaDTO? = null,

    @get: NotNull
    var lahetetty: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var erikoistuvaAllekirjoittanut: Boolean? = null

) : Serializable

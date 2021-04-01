package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class KoejaksonAloituskeskusteluDTO(

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
    var erikoistuvanSahkoposti: String? = null,

    @get: NotNull
    var koejaksonSuorituspaikka: String? = null,

    var koejaksonToinenSuorituspaikka: String? = null,

    @get: NotNull
    var koejaksonAlkamispaiva: LocalDate? = null,

    @get: NotNull
    var koejaksonPaattymispaiva: LocalDate? = null,

    @get: NotNull
    var suoritettuKokoaikatyossa: Boolean? = null,

    var tyotunnitViikossa: Double? = null,

    @get: NotNull
    var lahikouluttaja: KoejaksonKouluttajaDTO? = null,

    @get: NotNull
    var lahiesimies: KoejaksonKouluttajaDTO? = null,

    @get: NotNull
    var koejaksonOsaamistavoitteet: String? = null,

    @get: NotNull
    var lahetetty: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var korjausehdotus: String? = null,

    ) : Serializable

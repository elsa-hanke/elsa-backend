package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull

data class KoejaksonAloituskeskusteluDTO(

    var id: Long? = null,

    @get: NotNull
    var erikoistuvanNimi: String? = null,

    @get: NotNull
    var erikoistuvanErikoisala: String? = null,

    var erikoistuvanOpiskelijatunnus: String? = null,

    @get: NotNull
    var erikoistuvanYliopisto: String? = null,

    var erikoistuvanSahkoposti: String? = null,

    var erikoistuvanAvatar: ByteArray? = null,

    var koejaksonSuorituspaikka: String? = null,

    var koejaksonToinenSuorituspaikka: String? = null,

    var koejaksonAlkamispaiva: LocalDate? = null,

    var koejaksonPaattymispaiva: LocalDate? = null,

    var suoritettuKokoaikatyossa: Boolean? = null,

    var tyotunnitViikossa: Double? = null,

    var lahikouluttaja: KoejaksonKouluttajaDTO? = null,

    var lahiesimies: KoejaksonKouluttajaDTO? = null,

    var koejaksonOsaamistavoitteet: String? = null,

    var lahetetty: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var korjausehdotus: String? = null,

    var erikoistuvanKuittausaika: LocalDate? = null

) : Serializable {
    override fun toString() = "KoejaksonAloituskeskusteluDTO"
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Lob
import javax.validation.constraints.NotNull

data class KoejaksonLoppukeskusteluDTO(

    var id: Long? = null,

    @get: NotNull
    var erikoistuvanNimi: String? = null,

    @get: NotNull
    var erikoistuvanErikoisala: String? = null,

    @get: NotNull
    var erikoistuvanOpiskelijatunnus: String? = null,

    @get: NotNull
    var erikoistuvanYliopisto: String? = null,

    @Lob
    var erikoistuvanAvatar: ByteArray? = null,

    var esitetaanKoejaksonHyvaksymista: Boolean? = null,

    var jatkotoimenpiteet: String? = null,

    @get: NotNull
    var lahikouluttaja: KoejaksonKouluttajaDTO? = null,

    @get: NotNull
    var lahiesimies: KoejaksonKouluttajaDTO? = null,

    var muokkauspaiva: LocalDate? = null,

    var korjausehdotus: String? = null,

    var erikoistuvaAllekirjoittanut: Boolean? = null,

    var erikoistuvanAllekirjoitusaika: LocalDate? = null

) : Serializable {
    override fun toString() = "KoejaksonLoppukeskusteluDTO"
}

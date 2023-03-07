package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.KehittamistoimenpideKategoria
import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.NotNull

data class KoejaksonValiarviointiDTO(

    var id: Long? = null,

    @get: NotNull
    var erikoistuvanNimi: String? = null,

    @get: NotNull
    var erikoistuvanErikoisala: String? = null,

    var erikoistuvanOpiskelijatunnus: String? = null,

    @get: NotNull
    var erikoistuvanYliopisto: String? = null,

    var erikoistuvanAvatar: ByteArray? = null,

    var edistyminenTavoitteidenMukaista: Boolean? = null,

    var kehittamistoimenpideKategoriat: List<KehittamistoimenpideKategoria>? = null,

    var muuKategoria: String? = null,

    var vahvuudet: String? = null,

    var kehittamistoimenpiteet: String? = null,

    @get: NotNull
    var lahikouluttaja: KoejaksonKouluttajaDTO? = null,

    @get: NotNull
    var lahiesimies: KoejaksonKouluttajaDTO? = null,

    var muokkauspaiva: LocalDate? = null,

    var korjausehdotus: String? = null,

    var erikoistuvanKuittausaika: LocalDate? = null,

    var koejaksonOsaamistavoitteet: String? = null

) : Serializable {
    override fun toString() = "KoejaksonValiarviointiDTO"
}

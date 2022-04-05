package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.KehittamistoimenpideKategoria
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Lob
import javax.validation.constraints.NotNull

data class KoejaksonValiarviointiDTO(

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

    var erikoistuvanKuittausaika: LocalDate? = null

) : Serializable {
    override fun toString() = "KoejaksonValiarviointiDTO"
}

package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class KoejaksonKoulutussopimusDTO(

    var id: Long? = null,

    var erikoistuvanNimi: String? = null,

    var erikoistuvanOpiskelijatunnus: String? = null,

    var erikoistuvanSyntymaaika: LocalDate? = null,

    var erikoistuvanYliopisto: String? = null,

    var erikoistuvanErikoisala: String? = null,

    var erikoistuvanPuhelinnumero: String? = null,

    var erikoistuvanSahkoposti: String? = null,

    var erikoistuvanAvatar: ByteArray? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var koejaksonAlkamispaiva: LocalDate? = null,

    var lahetetty: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var vastuuhenkilo: KoulutussopimuksenVastuuhenkiloDTO? = null,

    var korjausehdotus: String? = null,

    var vastuuhenkilonKorjausehdotus: String? = null,

    var kouluttajat: MutableSet<KoulutussopimuksenKouluttajaDTO>? = mutableSetOf(),

    var koulutuspaikat: MutableSet<KoulutussopimuksenKoulutuspaikkaDTO>? = mutableSetOf(),

    var allekirjoitettu: Boolean? = null,

    var erikoistuvanAllekirjoitusaika: LocalDate? = null

) : Serializable {
    override fun toString() = "KoejaksonKoulutussopimusDTO"
}

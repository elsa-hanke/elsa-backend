package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Lob
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
    var erikoistuvanYliopistoId: Long? = null,

    var erikoistuvanErikoisala: String? = null,

    var erikoistuvanPuhelinnumero: String? = null,

    var erikoistuvanSahkoposti: String? = null,

    @Lob
    var erikoistuvanAvatar: ByteArray? = null,

    @get: NotNull
    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var koejaksonAlkamispaiva: LocalDate? = null,

    var lahetetty: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var vastuuhenkilo: KoulutussopimuksenVastuuhenkiloDTO? = null,

    var korjausehdotus: String? = null,

    var kouluttajat: MutableSet<KoulutussopimuksenKouluttajaDTO>? = mutableSetOf(),

    var koulutuspaikat: MutableSet<KoulutussopimuksenKoulutuspaikkaDTO>? = mutableSetOf(),

    // Lisätään allekirjoituksen logiikka sarakesign toteutuksen yhteydessä
    var allekirjoitettu: Boolean? = null,

    var erikoistuvanAllekirjoitusaika: LocalDate? = null

) : Serializable {
    override fun toString() = "KoejaksonKoulutussopimusDTO"
}

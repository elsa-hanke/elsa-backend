package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Lob
import javax.validation.constraints.NotNull

data class KoejaksonVastuuhenkilonArvioDTO(

    var id: Long? = null,

    var erikoistuvanNimi: String? = null,

    var erikoistuvanErikoisala: String? = null,

    var erikoistuvanOpiskelijatunnus: String? = null,

    var erikoistuvanYliopisto: String? = null,

    @Lob
    var erikoistuvanAvatar: ByteArray? = null,

    var vastuuhenkilo: KoejaksonKouluttajaDTO? = null,

    var koejaksoHyvaksytty: Boolean? = null,

    var perusteluHylkaamiselle: String? = null,

    var hylattyArviointiKaytyLapiKeskustellen: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var erikoistuvanAllekirjoitusaika: LocalDate? = null,

    var virkailija: KoejaksonKouluttajaDTO? = null,

    var lisatiedotVirkailijalta: String? = null,

    var allekirjoitettu: Boolean? = null

) : Serializable {
    override fun toString() = "KoejaksonVastuuhenkilonArvioDTO"
}

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

    var erikoistuvanPuhelinnumero: String? = null,

    var erikoistuvanSahkoposti: String? = null,

    @Lob
    var erikoistuvanAvatar: ByteArray? = null,

    var muutOpintooikeudet: List<OpintooikeusDTO>? = null,

    var koulutussopimusHyvaksytty: Boolean? = null,

    var vastuuhenkilo: KoejaksonKouluttajaDTO? = null,

    var vastuuhenkilonPuhelinnumero: String? = null,

    var vastuuhenkilonSahkoposti: String? = null,

    var koejaksoHyvaksytty: Boolean? = null,

    var perusteluHylkaamiselle: String? = null,

    var hylattyArviointiKaytyLapiKeskustellen: Boolean? = null,

    var muokkauspaiva: LocalDate? = null,

    var erikoistuvankuittausaika: LocalDate? = null,

    var virkailija: KoejaksonKouluttajaDTO? = null,

    var lisatiedotVirkailijalta: String? = null,

    var allekirjoitettu: Boolean? = null,

    var koejaksonSuorituspaikat: List<TyoskentelyjaksoDTO> = listOf(),

    var aloituskeskustelu: KoejaksonAloituskeskusteluDTO? = null,

    var valiarviointi: KoejaksonValiarviointiDTO? = null,

    var kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteetDTO? = null,

    var loppukeskustelu: KoejaksonLoppukeskusteluDTO? = null,

    var korjausehdotus: String? = null

) : Serializable {
    override fun toString() = "KoejaksonVastuuhenkilonArvioDTO"
}

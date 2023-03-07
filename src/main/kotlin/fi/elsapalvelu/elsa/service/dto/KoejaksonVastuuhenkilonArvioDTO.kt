package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import java.io.Serializable
import java.time.LocalDate

data class KoejaksonVastuuhenkilonArvioDTO(

    var id: Long? = null,

    var erikoistuvanNimi: String? = null,

    var erikoistuvanErikoisala: String? = null,

    var erikoistuvanOpiskelijatunnus: String? = null,

    var erikoistuvanYliopisto: String? = null,

    var erikoistuvanPuhelinnumero: String? = null,

    var erikoistuvanSahkoposti: String? = null,

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

    var erikoistuvanKuittausaika: LocalDate? = null,

    var virkailija: KoejaksonKouluttajaDTO? = null,

    var lisatiedotVirkailijalta: String? = null,

    var allekirjoitettu: Boolean? = null,

    var koejaksonSuorituspaikat: TyoskentelyjaksotTableDTO? = null,

    var aloituskeskustelu: KoejaksonAloituskeskusteluDTO? = null,

    var valiarviointi: KoejaksonValiarviointiDTO? = null,

    var kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteetDTO? = null,

    var loppukeskustelu: KoejaksonLoppukeskusteluDTO? = null,

    var korjausehdotus: String? = null,

    var tila: KoejaksoTila? = null

) : Serializable {
    override fun toString() = "KoejaksonVastuuhenkilonArvioDTO"
}

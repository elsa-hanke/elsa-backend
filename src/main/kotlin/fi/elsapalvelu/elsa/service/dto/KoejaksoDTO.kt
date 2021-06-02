package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import java.io.Serializable

data class KoejaksoDTO(

    var koulutussopimus: KoejaksonKoulutussopimusDTO? = null,

    var koulutusSopimuksenTila: KoejaksoTila? = null,

    var aloituskeskustelu: KoejaksonAloituskeskusteluDTO? = null,

    var aloituskeskustelunTila: KoejaksoTila? = null,

    var valiarviointi: KoejaksonValiarviointiDTO? = null,

    var valiarvioinninTila: KoejaksoTila? = null,

    var kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteetDTO? = null,

    var kehittamistoimenpiteidenTila: KoejaksoTila? = null,

    var loppukeskustelu: KoejaksonLoppukeskusteluDTO? = null,

    var loppukeskustelunTila: KoejaksoTila? = null,

    var vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvioDTO? = null,

    var vastuuhenkilonArvionTila: KoejaksoTila? = null,

    var kunnat: MutableList<KuntaDTO> = mutableListOf(),

    var erikoisalat: MutableList<ErikoisalaDTO> = mutableListOf(),

    var relatedTyoskentelyjaksot: MutableList<TyoskentelyjaksoDTO> = mutableListOf(),

    var allTyoskentelyjaksot: MutableList<TyoskentelyjaksoDTO> = mutableListOf()

) : Serializable

fun KoejaksoDTO.addKoulutussopimus(sopimus: KoejaksonKoulutussopimusDTO) {
    this.koulutussopimus = sopimus
    this.koulutusSopimuksenTila = KoejaksoTila.fromSopimus(sopimus)
}

fun KoejaksoDTO.addAloitusKeskustelu(
    aloituskeskustelu: KoejaksonAloituskeskusteluDTO
) {
    this.aloituskeskustelu = aloituskeskustelu
    this.aloituskeskustelunTila = KoejaksoTila.fromAloituskeskustelu(aloituskeskustelu)
}

fun KoejaksoDTO.addValiarviointi(
    valiarviointi: KoejaksonValiarviointiDTO
) {
    this.valiarviointi = valiarviointi
    this.valiarvioinninTila = KoejaksoTila.fromValiarvointi(true, valiarviointi)
}

fun KoejaksoDTO.addKehittamistoimenpiteet(
    kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteetDTO
) {
    this.kehittamistoimenpiteet = kehittamistoimenpiteet
    this.kehittamistoimenpiteidenTila =
        KoejaksoTila.fromKehittamistoimenpiteet(true, kehittamistoimenpiteet)
}

fun KoejaksoDTO.addLoppukeskustelu(
    loppukeskustelu: KoejaksonLoppukeskusteluDTO
) {
    this.loppukeskustelu = loppukeskustelu
    this.loppukeskustelunTila = KoejaksoTila.fromLoppukeskustelu(true, loppukeskustelu)
}

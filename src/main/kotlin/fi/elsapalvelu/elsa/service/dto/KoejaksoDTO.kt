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

    var kunnat: List<KuntaDTO> = listOf(),

    var erikoisalat: List<ErikoisalaDTO> = listOf(),

    var tyoskentelyjaksot: List<TyoskentelyjaksoDTO> = listOf()

) : Serializable {
    override fun toString() = "KoejaksoDTO"
}

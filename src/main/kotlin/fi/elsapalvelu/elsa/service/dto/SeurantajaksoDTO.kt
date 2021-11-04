package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate
import javax.validation.constraints.NotNull

data class SeurantajaksoDTO(

    var id: Long? = null,

    @get: NotNull
    var alkamispaiva: LocalDate? = null,

    @get: NotNull
    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    var omaArviointi: String? = null,

    var lisahuomioita: String? = null,

    var seuraavanJaksonTavoitteet: String? = null,

    var edistyminenTavoitteidenMukaista: Boolean? = null,

    var huolenaiheet: String? = null,

    var kouluttajanArvio: String? = null,

    var erikoisalanTyoskentelyvalmiudet: String? = null,

    var jatkotoimetJaRaportointi: String? = null,

    var hyvaksytty: Boolean? = null,

    var seurantakeskustelunYhteisetMerkinnat: String? = null,

    var seuraavanKeskustelunAjankohta: LocalDate? = null,

    var erikoistuvaLaakari: ErikoistuvaLaakariDTO? = null,

    var kouluttaja: KayttajaDTO? = null,

    var koulutusjaksot: MutableSet<KoulutusjaksoDTO>? = null,

    var korjausehdotus: String? = null

) : Serializable {
    override fun toString() = "SeurantajaksoDTO"
}

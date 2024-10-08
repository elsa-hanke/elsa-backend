package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import java.io.Serializable
import java.time.LocalDate

data class ErikoistujanEteneminenDTO(

    var opintooikeusId: Long? = null,

    var erikoistuvaLaakariEtuNimi: String? = null,

    var erikoistuvaLaakariSukuNimi: String? = null,

    var erikoistuvaLaakariSyntymaaika: LocalDate? = null,

    var tyoskentelyjaksoTilastot: TyoskentelyjaksotTilastotDTO? = null,

    var arviointienKeskiarvo: Double? = null,

    var arviointienLkm: Int? = null,

    var arvioitavienKokonaisuuksienLkm: Int? = null,

    var seurantajaksotLkm: Int? = null,

    var seurantajaksonHuoletLkm: Int? = null,

    var suoritemerkinnatLkm: Int? = null,

    var vaaditutSuoritemerkinnatLkm: Int? = null,

    var koejaksoTila: KoejaksoTila? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var opintooikeudenPaattymispaiva: LocalDate? = null,

    var asetus: String? = null,

    var erikoisala: String? = null,

    var terveyskeskuskoulutusjaksoSuoritettu: Boolean? = null,

    var laakarikoulutusSuoritettuSuomiTaiBelgia: Boolean? = false,

    var laakarikoulutusSuoritettuMuuKuinSuomiTaiBelgia: Boolean? = false

) : Serializable

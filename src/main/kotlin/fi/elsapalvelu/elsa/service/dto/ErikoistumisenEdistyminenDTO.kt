package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import java.io.Serializable
import java.time.LocalDate

data class ErikoistumisenEdistyminenDTO (

    var arviointienKeskiarvo: Double? = null,

    var arvioitavatKokonaisuudetVahintaanYksiArvioLkm: Int? = null,

    var arvioitavienKokonaisuuksienLkm: Int? = null,

    var arviointiasteikko: ArviointiasteikkoDTO? = null,

    var suoritemerkinnatLkm: Int? = null,

    var vaaditutSuoritemerkinnatLkm: Int? = null,

    var osaalueetSuoritettuLkm: Int? = null,

    var osaalueetVaadittuLkm: Int? = null,

    var tyoskentelyjaksoTilastot: TyoskentelyjaksotTilastotDTO? = null,

    var teoriakoulutuksetSuoritettu: Double? = null,

    var teoriakoulutuksetVaadittu: Double? = null,

    var johtamisopinnotSuoritettu: Double? = null,

    var johtamisopinnotVaadittu: Double? = null,

    var sateilysuojakoulutuksetSuoritettu: Double? = null,

    var sateilysuojakoulutuksetVaadittu: Double? = null,

    var koejaksoTila: KoejaksoTila? = null,

    var valtakunnallisetKuulustelutSuoritettuLkm: Int? = null,

    var opintooikeudenMyontamispaiva: LocalDate? = null,

    var opintooikeudenPaattymispaiva: LocalDate? = null

) : Serializable

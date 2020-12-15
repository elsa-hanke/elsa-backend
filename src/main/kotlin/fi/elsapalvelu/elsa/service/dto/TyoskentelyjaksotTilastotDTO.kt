package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksotTilastotDTO(

    var tyoskentelyaikaYhteensa: Double,

    var arvioErikoistumiseenHyvaksyttavista: Double,

    var arvioPuuttuvastaKoulutuksesta: Double,

    var koulutustyypit: TyoskentelyjaksotTilastotKoulutustyypitDTO,

    var kaytannonKoulutus: MutableSet<TyoskentelyjaksotTilastotKaytannonKoulutusDTO> = mutableSetOf(),

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksotTilastotTyoskentelyjaksotDTO> = mutableSetOf()

) : Serializable

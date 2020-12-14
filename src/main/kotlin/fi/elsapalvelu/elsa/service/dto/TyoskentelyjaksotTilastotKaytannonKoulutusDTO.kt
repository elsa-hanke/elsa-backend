package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import java.io.Serializable

class TyoskentelyjaksotTilastotKaytannonKoulutusDTO(

    var kaytannonKoulutus: KaytannonKoulutusTyyppi,

    var suoritettu: Double,

) : Serializable

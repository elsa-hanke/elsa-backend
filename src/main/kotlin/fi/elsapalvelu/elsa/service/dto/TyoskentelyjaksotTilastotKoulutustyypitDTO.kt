package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksotTilastotKoulutustyypitDTO(

    var terveyskeskusVaadittuVahintaan: Double,

    var terveyskeskusSuoritettu: Double,

    var yliopistosairaalaVaadittuVahintaan: Double,

    var yliopistosairaalaSuoritettu: Double,

    var yliopistosairaaloidenUlkopuolinenVaadittuVahintaan: Double,

    var yliopistosairaaloidenUlkopuolinenSuoritettu: Double,

    var yhteensaVaadittuVahintaan: Double,

    var yhteensaSuoritettu: Double

) : Serializable

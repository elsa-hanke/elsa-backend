package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class TyoskentelyjaksotKoulutustyypitDTO(

    var terveyskeskus: List<TyoskentelyjaksoDTO>,

    var yliopistosairaala: List<TyoskentelyjaksoDTO>,

    var yliopistosairaaloidenUlkopuolinen: List<TyoskentelyjaksoDTO>

) : Serializable {
    override fun toString() = "TyoskentelyjaksotTilastotDTO"
}

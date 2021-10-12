package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class KoulutusjaksoFormDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var kunnat: MutableSet<KuntaDTO> = mutableSetOf(),

    var arvioitavanKokonaisuudenKategoriat: MutableSet<ArvioitavanKokonaisuudenKategoriaDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "KoulutusjaksoFormDTO"
}

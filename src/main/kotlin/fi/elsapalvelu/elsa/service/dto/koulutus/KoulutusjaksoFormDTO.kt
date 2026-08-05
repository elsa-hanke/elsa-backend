package fi.elsapalvelu.elsa.service.dto.koulutus

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.arviointi.ArvioitavanKokonaisuudenKategoriaDTO
import fi.elsapalvelu.elsa.service.dto.perustiedot.KuntaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
class KoulutusjaksoFormDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var kunnat: MutableSet<KuntaDTO> = mutableSetOf(),

    var arvioitavanKokonaisuudenKategoriat: MutableSet<ArvioitavanKokonaisuudenKategoriaDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "KoulutusjaksoFormDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

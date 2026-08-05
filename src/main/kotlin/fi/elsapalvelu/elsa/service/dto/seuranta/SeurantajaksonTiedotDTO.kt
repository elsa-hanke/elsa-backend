package fi.elsapalvelu.elsa.service.dto.seuranta

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutusjaksoDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.TeoriakoulutusDTO
data class SeurantajaksonTiedotDTO(

    var koulutusjaksot: List<KoulutusjaksoDTO?>? = null,

    var arvioinnit: List<SeurantajaksonArviointiKategoriaDTO?>? = null,

    var arviointienMaara: Int? = null,

    var suoritemerkinnat: List<SeurantajaksonSuoritemerkintaDTO>? = null,

    var suoritemerkinnatMaara: Int? = null,

    var teoriakoulutukset: List<TeoriakoulutusDTO>? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
    override fun toString() = "SeurantajaksonTiedotDTO"
}

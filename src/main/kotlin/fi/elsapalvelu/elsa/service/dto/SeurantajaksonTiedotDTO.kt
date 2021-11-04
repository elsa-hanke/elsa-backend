package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class SeurantajaksonTiedotDTO(

    var osaamistavoitteet: List<String?>? = null,

    var muutOsaamistavoitteet: List<String?>? = null,

    var arvioinnit: List<SeurantajaksonArviointiKategoriaDTO?>? = null,

    var arviointienMaara: Int? = null,

    var suoritemerkinnat: List<SeurantajaksonSuoritemerkintaDTO>? = null,

    var suoritemerkinnatMaara: Int? = null,

    var teoriakoulutukset: List<TeoriakoulutusDTO>? = null

) : Serializable {
    override fun toString() = "SeurantajaksonTiedotDTO"
}

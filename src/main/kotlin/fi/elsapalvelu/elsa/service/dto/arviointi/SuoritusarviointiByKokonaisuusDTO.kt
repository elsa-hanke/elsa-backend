package fi.elsapalvelu.elsa.service.dto.arviointi

import java.io.Serializable
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
data class SuoritusarviointiByKokonaisuusDTO(

    var id: Long? = null,

    @get: NotNull
    var tapahtumanAjankohta: LocalDate? = null,

    var arvioitavaTapahtuma: String? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var itsearviointiArviointiasteikonTaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var arviointiasteikonTaso: Int? = null,

    var arvioinninAntaja: KayttajaDTO? = null,

    var arvioinninSaaja: KayttajaDTO? = null,

    var tyoskentelyjakso: TyoskentelyjaksoDTO? = null,

    var arviointiAsiakirjat: List<AsiakirjaDTO>? = listOf(),

    var itsearviointiAsiakirjat: List<AsiakirjaDTO>? = listOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiByKokonaisuusDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    companion object {
        private const val serialVersionUID = 1L
    }
}

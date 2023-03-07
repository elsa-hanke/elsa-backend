package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.ArvioinninPerustuminen
import java.io.Serializable
import java.time.LocalDate
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class SuoritusarviointiDTO(

    var id: Long? = null,

    @get: NotNull
    var tapahtumanAjankohta: LocalDate? = null,

    var arvioitavaTapahtuma: String? = null,

    var pyynnonAika: LocalDate? = null,

    var lisatiedot: String? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var itsearviointiVaativuustaso: Int? = null,

    var sanallinenItsearviointi: String? = null,

    var itsearviointiAika: LocalDate? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var vaativuustaso: Int? = null,

    var sanallinenArviointi: String? = null,

    var arviointiAika: LocalDate? = null,

    @get: NotNull
    var lukittu: Boolean = false,

    var kommentit: MutableSet<SuoritusarvioinninKommenttiDTO>? = null,

    @get: NotNull
    var arvioinninAntajaId: Long? = null,

    @get: NotNull
    var tyoskentelyjaksoId: Long? = null,

    var arvioinninSaaja: KayttajaDTO? = null,

    var arvioinninAntaja: KayttajaDTO? = null,

    var arvioitavatKokonaisuudet: MutableSet<SuoritusarvioinninArvioitavaKokonaisuusDTO>? = null,

    var tyoskentelyjakso: TyoskentelyjaksoDTO? = null,

    var arviointityokalut: Set<ArviointityokaluDTO>? = null,

    var arviointiasteikko: ArviointiasteikkoDTO? = null,

    var arviointiPerustuu: ArvioinninPerustuminen? = null,

    var muuPeruste: String? = null,

    var arviointiAsiakirja: AsiakirjaDTO? = null,

    var arviointiAsiakirjaUpdated: Boolean = false

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

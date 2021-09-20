package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.enumeration.ArvioinninPerustuminen
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Lob
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class SuoritusarviointiDTO(

    var id: Long? = null,

    @get: NotNull
    var tapahtumanAjankohta: LocalDate? = null,

    @get: NotNull
    var arvioitavaTapahtuma: String? = null,

    var pyynnonAika: LocalDate? = null,

    @Lob
    var lisatiedot: String? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var itsearviointiVaativuustaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var itsearviointiArviointiasteikonTaso: Int? = null,

    @Lob
    var sanallinenItsearviointi: String? = null,

    var itsearviointiAika: LocalDate? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var vaativuustaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var arviointiasteikonTaso: Int? = null,

    @Lob
    var sanallinenArviointi: String? = null,

    var arviointiAika: LocalDate? = null,

    var lukittu: Boolean = false,

    var kommentit: MutableSet<SuoritusarvioinninKommenttiDTO>? = null,

    var arvioinninAntajaId: Long? = null,

    var arvioitavaOsaalueId: Long? = null,

    var tyoskentelyjaksoId: Long? = null,

    var arvioinninSaaja: KayttajaDTO? = null,

    var arvioinninAntaja: KayttajaDTO? = null,

    var arvioitavaOsaalue: ArvioitavaKokonaisuusDTO? = null,

    var tyoskentelyjakso: TyoskentelyjaksoDTO? = null,

    var arviointityokalut: Set<ArviointityokaluDTO>? = null,

    var arviointiPerustuu: ArvioinninPerustuminen? = null,

    var muuPeruste: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

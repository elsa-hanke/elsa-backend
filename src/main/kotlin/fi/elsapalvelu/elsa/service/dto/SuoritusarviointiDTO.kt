package fi.elsapalvelu.elsa.service.dto

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

    @get: NotNull
    var pyynnonAika: LocalDate? = null,

    @Lob
    var lisatiedot: String? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var itsearviointiVaativuustaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var itsearviointiLuottamuksenTaso: Int? = null,

    @Lob
    var sanallinenItsearviointi: String? = null,

    var itsearviointiAika: LocalDate? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var vaativuustaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var luottamuksenTaso: Int? = null,

    @Lob
    var sanallinenArviointi: String? = null,

    var arviointiAika: LocalDate? = null,

    var lukittu: Boolean = false,

    var kommentit: MutableSet<SuoritusarvioinninKommenttiDTO>? = null,

    var arvioinninAntajaId: Long? = null,

    var arvioitavaOsaalueId: Long? = null,

    var tyoskentelyjaksoId: Long? = null,

    var arvioinninAntaja: KayttajaDTO? = null,

    var arvioitavaOsaalue: EpaOsaamisalueDTO? = null,

    var tyoskentelyjakso: TyoskentelyjaksoDTO? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

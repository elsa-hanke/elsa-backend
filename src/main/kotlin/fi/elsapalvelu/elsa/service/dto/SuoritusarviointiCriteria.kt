package fi.elsapalvelu.elsa.service.dto

import io.github.jhipster.service.Criteria
import io.github.jhipster.service.filter.IntegerFilter
import io.github.jhipster.service.filter.LocalDateFilter
import io.github.jhipster.service.filter.LongFilter
import io.github.jhipster.service.filter.StringFilter
import java.io.Serializable

data class SuoritusarviointiCriteria(

    var id: LongFilter? = null,

    var tapahtumanAjankohta: LocalDateFilter? = null,

    var arvioitavaTapahtuma: StringFilter? = null,

    var pyynnonAika: LocalDateFilter? = null,

    var vaativuustaso: IntegerFilter? = null,

    var sanallinenArviointi: StringFilter? = null,

    var arviointiAika: LocalDateFilter? = null,

    var tyoskentelyjaksoId: LongFilter? = null,

    var arvioitavaOsaalueId: LongFilter? = null,

    var arvioinninAntajaId: LongFilter? = null

) : Serializable, Criteria {

    constructor(other: SuoritusarviointiCriteria) :
        this(
            other.id?.copy(),
            other.tapahtumanAjankohta?.copy(),
            other.arvioitavaTapahtuma?.copy(),
            other.pyynnonAika?.copy(),
            other.vaativuustaso?.copy(),
            other.sanallinenArviointi?.copy(),
            other.arviointiAika?.copy(),
            other.tyoskentelyjaksoId?.copy(),
            other.arvioitavaOsaalueId?.copy(),
            other.arvioinninAntajaId?.copy()
        )

    override fun copy() = SuoritusarviointiCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

package fi.elsapalvelu.elsa.service.criteria

import fi.elsapalvelu.elsa.domain.VastuuhenkilonTehtavatyyppi
import tech.jhipster.service.Criteria
import tech.jhipster.service.filter.BooleanFilter
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter
import java.io.Serializable

data class KayttajahallintaCriteria(

    var nimi: StringFilter? = null,

    var erikoisalaId: LongFilter? = null,

    var useaOpintooikeus: BooleanFilter? = null,

    var vastuuhenkilonVastuualue: VastuuhenkilonTehtavatyyppi? = null

) : Serializable, Criteria {

    constructor(other: KayttajahallintaCriteria) :
        this(
            other.nimi?.copy(),
            other.erikoisalaId?.copy(),
            other.useaOpintooikeus?.copy(),
            other.vastuuhenkilonVastuualue?.copy()
        )

    override fun copy() = KayttajahallintaCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}


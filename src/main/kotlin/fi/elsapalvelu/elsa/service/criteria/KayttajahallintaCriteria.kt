package fi.elsapalvelu.elsa.service.criteria

import tech.jhipster.service.Criteria
import tech.jhipster.service.filter.BooleanFilter
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter
import java.io.Serializable

data class KayttajahallintaCriteria(

    var nimi: StringFilter? = null,

    var erikoisalaId: LongFilter? = null,

    var useaOpintooikeus: BooleanFilter? = null,

    var vastuuhenkilonTehtavatyyppiId: LongFilter? = null

) : Serializable, Criteria {

    constructor(other: KayttajahallintaCriteria) :
        this(
            other.nimi?.copy(),
            other.erikoisalaId?.copy(),
            other.useaOpintooikeus?.copy(),
            other.vastuuhenkilonTehtavatyyppiId?.copy()
        )

    override fun copy() = KayttajahallintaCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}


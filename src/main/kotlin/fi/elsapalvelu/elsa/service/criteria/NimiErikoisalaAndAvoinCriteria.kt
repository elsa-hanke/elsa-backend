package fi.elsapalvelu.elsa.service.criteria

import tech.jhipster.service.Criteria
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter
import java.io.Serializable

data class NimiErikoisalaAndAvoinCriteria(

    var erikoistujanNimi: StringFilter? = null,

    var erikoisalaId: LongFilter? = null,

    var avoin: Boolean? = null

) : Serializable, Criteria {

    constructor(other: NimiErikoisalaAndAvoinCriteria) :
        this(
            other.erikoistujanNimi?.copy(),
            other.erikoisalaId?.copy(),
            other.avoin
        )

    override fun copy() = NimiErikoisalaAndAvoinCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

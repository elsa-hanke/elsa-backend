package fi.elsapalvelu.elsa.service.criteria

import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import tech.jhipster.service.Criteria
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter
import java.io.Serializable

data class ErikoistujanEteneminenCriteria(

    var nimi: StringFilter? = null,

    var erikoisalaId: LongFilter? = null,

    var asetusId: LongFilter? = null,

    var naytaPaattyneet: Boolean? = null

) : Serializable, Criteria {

    constructor(other: ErikoistujanEteneminenCriteria) :
        this(
            other.nimi?.copy(),
            other.erikoisalaId?.copy(),
            other.asetusId?.copy()
        )

    override fun copy() = ErikoistujanEteneminenCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

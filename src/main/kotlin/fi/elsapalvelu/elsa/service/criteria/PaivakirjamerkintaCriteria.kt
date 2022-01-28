package fi.elsapalvelu.elsa.service.criteria

import tech.jhipster.service.Criteria
import tech.jhipster.service.filter.BooleanFilter
import tech.jhipster.service.filter.LocalDateFilter
import tech.jhipster.service.filter.LongFilter
import tech.jhipster.service.filter.StringFilter
import java.io.Serializable

data class PaivakirjamerkintaCriteria(

    var id: LongFilter? = null,

    var paivamaara: LocalDateFilter? = null,

    var oppimistapahtumanNimi: StringFilter? = null,

    var muunAiheenNimi: StringFilter? = null,

    var yksityinen: BooleanFilter? = null,

    var aihekategoriaId: LongFilter? = null,

    var teoriakoulutusId: LongFilter? = null

) : Serializable, Criteria {

    constructor(other: PaivakirjamerkintaCriteria) :
        this(
            other.id?.copy(),
            other.paivamaara?.copy(),
            other.oppimistapahtumanNimi?.copy(),
            other.muunAiheenNimi?.copy(),
            other.yksityinen?.copy(),
            other.aihekategoriaId?.copy(),
            other.teoriakoulutusId?.copy()
        )

    override fun copy() = PaivakirjamerkintaCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

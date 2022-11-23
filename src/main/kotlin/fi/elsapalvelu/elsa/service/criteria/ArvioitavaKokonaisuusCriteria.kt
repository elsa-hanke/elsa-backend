package fi.elsapalvelu.elsa.service.criteria

import tech.jhipster.service.Criteria
import java.io.Serializable

data class ArvioitavaKokonaisuusCriteria(

    var erikoisalaId: Long? = null,

    var voimassaolevat: Boolean? = null

) : Serializable, Criteria {

    constructor(other: ArvioitavaKokonaisuusCriteria) :
        this(
            other.erikoisalaId,
            other.voimassaolevat
        )

    override fun copy() = ArvioitavaKokonaisuusCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

package fi.elsapalvelu.elsa.service.criteria

import tech.jhipster.service.Criteria
import java.io.Serializable

data class AvoinAndNimiCriteria(

    var avoin: Boolean = true,
    var nimi: String = ""

) : Serializable, Criteria {

    constructor(other: AvoinAndNimiCriteria) :
        this(
            other.avoin,
            other.nimi
        )

    override fun copy() = AvoinAndNimiCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

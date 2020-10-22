package fi.elsapalvelu.elsa.service.dto

import io.github.jhipster.service.Criteria
import io.github.jhipster.service.filter.IntegerFilter
import io.github.jhipster.service.filter.LocalDateFilter
import io.github.jhipster.service.filter.StringFilter
import java.io.Serializable

data class SoteOrganisaatioCriteria(

    var organizationId: StringFilter? = null,

    var abbreviation: StringFilter? = null,

    var longName: StringFilter? = null,

    var parentId: StringFilter? = null,

    var hierarchyLevel: IntegerFilter? = null,

    var beginningDate: LocalDateFilter? = null,

    var expiringDate: LocalDateFilter? = null,

    var lastModifiedDate: LocalDateFilter? = null,

    var description: StringFilter? = null,

    var oid: StringFilter? = null,

    var costCenter: StringFilter? = null,

    var postAddress: StringFilter? = null,

    var streetAddress: StringFilter? = null,

    var postNumber: StringFilter? = null,

    var postOffice: StringFilter? = null,

    var phoneNumber: StringFilter? = null,

    var faxNumber: StringFilter? = null,

    var createdDate: LocalDateFilter? = null
) : Serializable, Criteria {

    constructor(other: SoteOrganisaatioCriteria) :
        this(
            other.organizationId?.copy(),
            other.abbreviation?.copy(),
            other.longName?.copy(),
            other.parentId?.copy(),
            other.hierarchyLevel?.copy(),
            other.beginningDate?.copy(),
            other.expiringDate?.copy(),
            other.lastModifiedDate?.copy(),
            other.description?.copy(),
            other.oid?.copy(),
            other.costCenter?.copy(),
            other.postAddress?.copy(),
            other.streetAddress?.copy(),
            other.postNumber?.copy(),
            other.postOffice?.copy(),
            other.phoneNumber?.copy(),
            other.faxNumber?.copy(),
            other.createdDate?.copy()
        )

    override fun copy() = SoteOrganisaatioCriteria(this)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

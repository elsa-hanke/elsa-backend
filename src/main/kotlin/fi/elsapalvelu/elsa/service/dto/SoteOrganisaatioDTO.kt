package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.time.LocalDate

data class SoteOrganisaatioDTO(

    var organizationId: String? = null,

    var abbreviation: String? = null,

    var longName: String? = null,

    var parentId: String? = null,

    var hierarchyLevel: Int? = null,

    var beginningDate: LocalDate? = null,

    var expiringDate: LocalDate? = null,

    var lastModifiedDate: LocalDate? = null,

    var description: String? = null,

    var oid: String? = null,

    var costCenter: String? = null,

    var postAddress: String? = null,

    var streetAddress: String? = null,

    var postNumber: String? = null,

    var postOffice: String? = null,

    var phoneNumber: String? = null,

    var faxNumber: String? = null,

    var createdDate: LocalDate? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SoteOrganisaatioDTO) return false
        return organizationId != null && organizationId == other.organizationId
    }

    override fun hashCode() = 31
}

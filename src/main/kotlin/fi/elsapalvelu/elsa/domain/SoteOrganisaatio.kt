package fi.elsapalvelu.elsa.domain

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

@Entity
@Table(name = "sote_organisaatio")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class SoteOrganisaatio(

    @Id
    @Column(name = "organization_id")
    var organizationId: String? = null,

    @Column(name = "abbreviation")
    var abbreviation: String? = null,

    @Column(name = "long_name")
    var longName: String? = null,

    @Column(name = "parent_id")
    var parentId: String? = null,

    @Column(name = "hierarchy_level")
    var hierarchyLevel: Int? = null,

    @Column(name = "beginning_date")
    var beginningDate: LocalDate? = null,

    @Column(name = "expiring_date")
    var expiringDate: LocalDate? = null,

    @Column(name = "last_modified_date")
    var lastModifiedDate: LocalDate? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "oid")
    var oid: String? = null,

    @Column(name = "cost_center")
    var costCenter: String? = null,

    @Column(name = "post_address")
    var postAddress: String? = null,

    @Column(name = "street_address")
    var streetAddress: String? = null,

    @Column(name = "post_number")
    var postNumber: String? = null,

    @Column(name = "post_office")
    var postOffice: String? = null,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    @Column(name = "fax_number")
    var faxNumber: String? = null,

    @Column(name = "created_date")
    var createdDate: LocalDate? = null

) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SoteOrganisaatio) return false

        return organizationId != null && other.organizationId != null && organizationId == other.organizationId
    }

    override fun hashCode() = 31

    override fun toString() = "SoteOrganisaatio{" +
        "organizationId='$organizationId'" +
        ", abbreviation='$abbreviation'" +
        ", longName='$longName'" +
        ", parentId='$parentId'" +
        ", hierarchyLevel=$hierarchyLevel" +
        ", beginningDate='$beginningDate'" +
        ", expiringDate='$expiringDate'" +
        ", lastModifiedDate='$lastModifiedDate'" +
        ", description='$description'" +
        ", oid='$oid'" +
        ", costCenter='$costCenter'" +
        ", postAddress='$postAddress'" +
        ", streetAddress='$streetAddress'" +
        ", postNumber='$postNumber'" +
        ", postOffice='$postOffice'" +
        ", phoneNumber='$phoneNumber'" +
        ", faxNumber='$faxNumber'" +
        ", createdDate='$createdDate'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

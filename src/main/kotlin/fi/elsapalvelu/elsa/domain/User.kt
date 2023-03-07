package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import fi.elsapalvelu.elsa.config.LOGIN_REGEX
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.GenericGenerator
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.io.Serializable
import java.time.Instant
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Entity
@Table(name = "jhi_user")
@Audited
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class User(

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    var id: String? = null,

    @field:NotNull
    @field:Pattern(regexp = LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    @Column(length = 50, unique = true, nullable = false)
    var login: String? = null,

    @field:Size(max = 50)
    @Column(name = "first_name", length = 50)
    var firstName: String? = null,

    @field:Size(max = 50)
    @Column(name = "last_name", length = 50)
    var lastName: String? = null,

    @field:Email
    @field:Size(min = 5, max = 254)
    @Column(length = 254, unique = true)
    var email: String? = null,

    @field:Size(max = 254)
    @Column(name = "phone_number", length = 254)
    var phoneNumber: String? = null,

    @Column(name = "avatar")
    var avatar: ByteArray? = null,

    @field:NotNull
    @Column(nullable = false)
    var activated: Boolean = false,

    @Column
    @NotAudited
    var hetu: ByteArray? = null,

    @field:Size(max = 254)
    @Column(name = "eppn", length = 254)
    var eppn: String? = null,

    @NotAudited
    @Column(name = "init_vector")
    var initVector: ByteArray? = null,

    @field:Size(min = 2, max = 10)
    @Column(name = "lang_key", length = 10)
    var langKey: String? = null,

    @JsonIgnore
    @ManyToMany
    @NotAudited
    @JoinTable(
        name = "jhi_user_authority",
        joinColumns = [JoinColumn(name = "user_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "authority_name", referencedColumnName = "name")]
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @BatchSize(size = 20)
    var authorities: MutableSet<Authority> = mutableSetOf(),

    createdBy: String? = null,
    createdDate: Instant? = Instant.now(),
    lastModifiedBy: String? = null,
    lastModifiedDate: Instant? = Instant.now()
) : AbstractAuditingEntity(createdBy, createdDate, lastModifiedBy, lastModifiedDate), Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        if (other.id == null || id == null) return false

        return id == other.id
    }

    override fun hashCode() = 31

    override fun toString() =
        "User{" +
            "login='$login'" +
            ", firstName='$firstName'" +
            ", lastName='$lastName'" +
            ", email='$email'" +
            ", phoneNumber='$phoneNumber'" +
            ", activated='$activated'" +
            ", langKey='$langKey'" +
            "}"

    companion object {
        private const val serialVersionUID = 1L
    }

    fun getName(): String {
        return "$firstName $lastName"
    }
}

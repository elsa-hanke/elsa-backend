package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.GenericGenerator
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "verification_token")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class VerificationToken(

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    var id: String? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    var user: User? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VerificationToken) return false
        if (other.id == null || id == null) return false

        return id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "VerificationToken{id=$id}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

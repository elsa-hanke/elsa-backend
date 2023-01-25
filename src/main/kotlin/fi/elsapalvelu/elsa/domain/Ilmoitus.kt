package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "ilmoitus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Ilmoitus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "teksti", nullable = false)
    var teksti: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ilmoitus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Ilmoitus{" +
        "id=$id" +
        ", teksti='$teksti'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

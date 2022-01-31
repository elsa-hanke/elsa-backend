package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "asetus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Asetus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asetus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Asetus{" +
        "id=$id" +
        ", nimi='$nimi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

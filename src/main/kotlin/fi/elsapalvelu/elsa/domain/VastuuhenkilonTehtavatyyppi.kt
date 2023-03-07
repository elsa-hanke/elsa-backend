package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import java.io.Serializable
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "vastuuhenkilon_tehtavatyyppi")
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class VastuuhenkilonTehtavatyyppi(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name="nimi")
    var nimi: VastuuhenkilonTehtavatyyppiEnum? = null

): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VastuuhenkilonTehtavatyyppi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "VastuuhenkilonTehtavatyyppi{id=$id, nimi=$nimi}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

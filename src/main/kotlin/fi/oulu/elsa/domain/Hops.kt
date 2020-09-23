package fi.oulu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*

/**
 * A Hops.
 */
@Entity
@Table(name = "hops")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Hops(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,
    @Column(name = "suunnitelman_tunnus")
    var suunnitelmanTunnus: String? = null,

    @OneToOne @JoinColumn(unique = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hops) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Hops{" +
        "id=$id" +
        ", suunnitelmanTunnus='$suunnitelmanTunnus'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

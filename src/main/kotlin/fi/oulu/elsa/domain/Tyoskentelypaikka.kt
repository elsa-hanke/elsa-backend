package fi.oulu.elsa.domain

import fi.oulu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*

/**
 * A Tyoskentelypaikka.
 */
@Entity
@Table(name = "tyoskentelypaikka")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Tyoskentelypaikka(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,
    @Column(name = "nimi")
    var nimi: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "tyyppi")
    var tyyppi: TyoskentelyjaksoTyyppi? = null,

    @OneToOne @JoinColumn(unique = true)
    var tyoskentelyjakso: Tyoskentelyjakso? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tyoskentelypaikka) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Tyoskentelypaikka{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", tyyppi='$tyyppi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.oulu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*

/**
 * A PikaviestiKeskustelu.
 */
@Entity
@Table(name = "pikaviesti_keskustelu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class PikaviestiKeskustelu(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,
    @Column(name = "aihe")
    var aihe: String? = null,

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "pikaviesti_keskustelu_keskustelija",
        joinColumns = [JoinColumn(name = "pikaviesti_keskustelu_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "keskustelija_id", referencedColumnName = "id")]
    )
    var keskustelijas: MutableSet<Kayttaja> = mutableSetOf()

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {

    fun addKeskustelija(kayttaja: Kayttaja): PikaviestiKeskustelu {
        this.keskustelijas.add(kayttaja)
        return this
    }

    fun removeKeskustelija(kayttaja: Kayttaja): PikaviestiKeskustelu {
        this.keskustelijas.remove(kayttaja)
        return this
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PikaviestiKeskustelu) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "PikaviestiKeskustelu{" +
        "id=$id" +
        ", aihe='$aihe'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

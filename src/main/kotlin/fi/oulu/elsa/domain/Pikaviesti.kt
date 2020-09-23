package fi.oulu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*

/**
 * A Pikaviesti.
 */
@Entity
@Table(name = "pikaviesti")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Pikaviesti(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,
    @Column(name = "sisalto")
    var sisalto: String? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["pikaviestis"], allowSetters = true)
    var keskustelu: PikaviestiKeskustelu? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["pikaviestis"], allowSetters = true)
    var lahettaja: Kayttaja? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pikaviesti) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Pikaviesti{" +
        "id=$id" +
        ", sisalto='$sisalto'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

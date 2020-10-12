package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*

/**
 * A Koejakso.
 */
@Entity
@Table(name = "koejakso")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Koejakso(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "ohjeteksti")
    var ohjeteksti: String? = null,

    @Column(name = "alkamispaiva")
    var alkamispaiva: LocalDate? = null,

    @Column(name = "paattymispaiva")
    var paattymispaiva: LocalDate? = null,

    @OneToOne @JoinColumn(unique = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["koejaksos"], allowSetters = true)
    var lahikouluttaja: Kayttaja? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["koejaksos"], allowSetters = true)
    var vastuuhenkilo: Kayttaja? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Koejakso) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Koejakso{" +
        "id=$id" +
        ", ohjeteksti='$ohjeteksti'" +
        ", alkamispaiva='$alkamispaiva'" +
        ", paattymispaiva='$paattymispaiva'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

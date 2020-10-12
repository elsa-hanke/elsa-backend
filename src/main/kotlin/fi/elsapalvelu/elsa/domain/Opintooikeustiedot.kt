package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*

/**
 * A Opintooikeustiedot.
 */
@Entity
@Table(name = "opintooikeustiedot")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Opintooikeustiedot(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "voimassaolo_alkaa")
    var voimassaoloAlkaa: LocalDate? = null,

    @Column(name = "voimassaolo_paattyy")
    var voimassaoloPaattyy: LocalDate? = null,

    @Column(name = "erikoisala")
    var erikoisala: String? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["opintooikeustiedots"], allowSetters = true)
    var yliopisto: Yliopisto? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Opintooikeustiedot) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Opintooikeustiedot{" +
        "id=$id" +
        ", voimassaoloAlkaa='$voimassaoloAlkaa'" +
        ", voimassaoloPaattyy='$voimassaoloPaattyy'" +
        ", erikoisala='$erikoisala'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

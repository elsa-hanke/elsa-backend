package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

/**
 * A Tyoskentelyjakso.
 */
@Entity
@Table(name = "tyoskentelyjakso")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Tyoskentelyjakso(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "tunnus", nullable = false)
    var tunnus: String? = null,

    @Column(name = "osasto")
    var osasto: String? = null,

    @Column(name = "alkamispaiva")
    var alkamispaiva: LocalDate? = null,

    @Column(name = "paattymispaiva")
    var paattymispaiva: LocalDate? = null,

    @OneToOne(mappedBy = "tyoskentelyjakso")
    @JsonIgnore
    var tyoskentelypaikka: Tyoskentelypaikka? = null,

    @OneToOne(mappedBy = "tyoskentelyjakso")
    @JsonIgnore
    var arviointi: OsaamisenArviointi? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["tyoskentelyjaksos"], allowSetters = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tyoskentelyjakso) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Tyoskentelyjakso{" +
        "id=$id" +
        ", tunnus='$tunnus'" +
        ", osasto='$osasto'" +
        ", alkamispaiva='$alkamispaiva'" +
        ", paattymispaiva='$paattymispaiva'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

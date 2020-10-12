package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*

/**
 * A OsaamisenArviointi.
 */
@Entity
@Table(name = "osaamisen_arviointi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class OsaamisenArviointi(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "tunnus")
    var tunnus: String? = null,

    @Column(name = "osasto")
    var osasto: String? = null,

    @Column(name = "alkamispaiva")
    var alkamispaiva: LocalDate? = null,

    @Column(name = "paattymispaiva")
    var paattymispaiva: LocalDate? = null,

    @OneToOne @JoinColumn(unique = true)
    var tyoskentelyjakso: Tyoskentelyjakso? = null,

    @OneToMany(mappedBy = "osaamisenArviointi")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var osaamisalueenArviointis: MutableSet<OsaamisalueenArviointi> = mutableSetOf(),

    @ManyToOne @JsonIgnoreProperties(value = ["osaamisenArviointis"], allowSetters = true)
    var arvioitava: ErikoistuvaLaakari? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["osaamisenArviointis"], allowSetters = true)
    var arvioija: Kayttaja? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {

    fun addOsaamisalueenArviointi(osaamisalueenArviointi: OsaamisalueenArviointi): OsaamisenArviointi {
        this.osaamisalueenArviointis.add(osaamisalueenArviointi)
        osaamisalueenArviointi.osaamisenArviointi = this
        return this
    }

    fun removeOsaamisalueenArviointi(osaamisalueenArviointi: OsaamisalueenArviointi): OsaamisenArviointi {
        this.osaamisalueenArviointis.remove(osaamisalueenArviointi)
        osaamisalueenArviointi.osaamisenArviointi = null
        return this
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OsaamisenArviointi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "OsaamisenArviointi{" +
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

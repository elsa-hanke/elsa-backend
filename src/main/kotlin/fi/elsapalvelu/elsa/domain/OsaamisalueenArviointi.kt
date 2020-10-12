package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

/**
 * A OsaamisalueenArviointi.
 */
@Entity
@Table(name = "osaamisalueen_arviointi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class OsaamisalueenArviointi(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "vaatimustaso")
    var vaatimustaso: Int? = null,

    @get: Min(value = 0)
    @get: Max(value = 5)
    @Column(name = "arvio")
    var arvio: Int? = null,

    @Column(name = "sanallinen_arvio")
    var sanallinenArvio: String? = null,

    @OneToMany(mappedBy = "osaamisalueenArviointi")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var arviointiosaalues: MutableSet<Arviointiosaalue> = mutableSetOf(),

    @ManyToOne @JsonIgnoreProperties(value = ["osaamisalueenArviointis"], allowSetters = true)
    var osaamisenArviointi: OsaamisenArviointi? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {

    fun addArviointiosaalue(arviointiosaalue: Arviointiosaalue): OsaamisalueenArviointi {
        this.arviointiosaalues.add(arviointiosaalue)
        arviointiosaalue.osaamisalueenArviointi = this
        return this
    }

    fun removeArviointiosaalue(arviointiosaalue: Arviointiosaalue): OsaamisalueenArviointi {
        this.arviointiosaalues.remove(arviointiosaalue)
        arviointiosaalue.osaamisalueenArviointi = null
        return this
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OsaamisalueenArviointi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "OsaamisalueenArviointi{" +
        "id=$id" +
        ", vaatimustaso=$vaatimustaso" +
        ", arvio=$arvio" +
        ", sanallinenArvio='$sanallinenArvio'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

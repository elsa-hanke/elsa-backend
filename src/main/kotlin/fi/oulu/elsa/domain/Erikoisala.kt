package fi.oulu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

/**
 * A Erikoisala.
 */
@Entity
@Table(name = "erikoisala")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Erikoisala(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @ManyToMany(mappedBy = "erikoisalas")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnore
    var yliopistos: MutableSet<Yliopisto> = mutableSetOf()

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {

    fun addYliopisto(yliopisto: Yliopisto): Erikoisala {
        this.yliopistos.add(yliopisto)
        yliopisto.erikoisalas.add(this)
        return this
    }

    fun removeYliopisto(yliopisto: Yliopisto): Erikoisala {
        this.yliopistos.remove(yliopisto)
        yliopisto.erikoisalas.remove(this)
        return this
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Erikoisala) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Erikoisala{" +
        "id=$id" +
        ", nimi='$nimi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

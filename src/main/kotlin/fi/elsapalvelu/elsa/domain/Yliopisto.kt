package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "yliopisto")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Yliopisto(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "yliopisto_erikoisala",
        joinColumns = [JoinColumn(name = "yliopisto_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "erikoisala_id", referencedColumnName = "id")]
    )
    var erikoisalat: MutableSet<Erikoisala> = mutableSetOf()

) : Serializable {

    fun addErikoisala(erikoisala: Erikoisala): Yliopisto {
        this.erikoisalat.add(erikoisala)
        return this
    }

    fun removeErikoisala(erikoisala: Erikoisala): Yliopisto {
        this.erikoisalat.remove(erikoisala)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Yliopisto) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Yliopisto{" +
        "id=$id" +
        ", nimi='$nimi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

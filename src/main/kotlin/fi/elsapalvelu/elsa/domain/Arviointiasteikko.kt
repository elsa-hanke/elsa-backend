package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikkoTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "arviointiasteikko")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Arviointiasteikko(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nimi", nullable = false)
    var nimi: ArviointiasteikkoTyyppi? = null,

    @OneToMany(mappedBy = "arviointiasteikko")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var tasot: MutableSet<ArviointiasteikonTaso> = mutableSetOf(),

    @OneToMany(mappedBy = "arviointiasteikko")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var opintooppaat: MutableSet<Opintoopas> = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Arviointiasteikko) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString(): String {
        return "Arviointiasteikko(id=$id, nimi=$nimi, tasot=$tasot)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*

@Entity
@Table(name = "koejakso")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Koejakso(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @OneToOne(mappedBy = "koejakso")
    @JsonIgnore
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @Column(name = "alkamispaiva")
    var alkamispaiva: LocalDate? = null,

    @Column(name = "paattymispaiva")
    var paattymispaiva: LocalDate? = null,

    @OneToOne(mappedBy = "koejakso")
    var koulutussopimus: KoejaksonKoulutussopimus? = null,

    @OneToMany(mappedBy = "koejakso")
    var tyoskentelyjaksot: MutableSet<Tyoskentelyjakso> = mutableSetOf()

) : Serializable {
    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Koejakso

        if (id != other.id) return false
        if (erikoistuvaLaakari != other.erikoistuvaLaakari) return false
        if (alkamispaiva != other.alkamispaiva) return false
        if (paattymispaiva != other.paattymispaiva) return false
        if (koulutussopimus != other.koulutussopimus) return false
        if (tyoskentelyjaksot != other.tyoskentelyjaksot) return false

        return true
    }

    override fun toString(): String {
        return "Koejakso(id=$id, erikoistuvaLaakari=$erikoistuvaLaakari, alkamispaiva=$alkamispaiva, paattymispaiva=$paattymispaiva, koulutussopimus=$koulutussopimus, tyoskentelyjaksot=$tyoskentelyjaksot)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

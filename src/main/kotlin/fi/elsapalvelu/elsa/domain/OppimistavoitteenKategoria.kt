package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "oppimistavoitteen_kategoria")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class OppimistavoitteenKategoria(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @get: NotNull
    @Column(name = "voimassaolon_alkamispaiva", nullable = false)
    var voimassaolonAlkamispaiva: LocalDate? = null,

    @Column(name = "voimassaolon_paattymispaiva")
    var voimassaolonPaattymispaiva: LocalDate? = null,

    @OneToMany(mappedBy = "kategoria")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var oppimistavoitteet: MutableSet<Oppimistavoite> = mutableSetOf()

) : Serializable {

    fun addOppimistavoite(oppimistavoite: Oppimistavoite): OppimistavoitteenKategoria {
        this.oppimistavoitteet.add(oppimistavoite)
        oppimistavoite.kategoria = this
        return this
    }

    fun removeOppimistavoite(oppimistavoite: Oppimistavoite): OppimistavoitteenKategoria {
        this.oppimistavoitteet.remove(oppimistavoite)
        oppimistavoite.kategoria = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OppimistavoitteenKategoria) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Oppimistavoite{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", voimassaolonAlkamispaiva='$voimassaolonAlkamispaiva'" +
        ", voimassaolonPaattymispaiva='$voimassaolonPaattymispaiva'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

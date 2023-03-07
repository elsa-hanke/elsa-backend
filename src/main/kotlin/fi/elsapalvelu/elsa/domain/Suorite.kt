package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "suorite")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class Suorite(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "nimi_sv")
    var nimiSv: String? = null,

    @get: NotNull
    @Column(name = "voimassaolon_alkamispaiva", nullable = false)
    var voimassaolonAlkamispaiva: LocalDate? = null,

    @Column(name = "voimassaolon_paattymispaiva")
    var voimassaolonPaattymispaiva: LocalDate? = null,

    @NotNull
    @ManyToOne(optional = false)
    var kategoria: SuoritteenKategoria? = null,

    @Column(name = "vaadittu_lkm")
    var vaadittulkm: Int? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Suorite) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Suorite{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", voimassaolonAlkamispaiva='$voimassaolonAlkamispaiva'" +
        ", voimassaolonPaattymispaiva='$voimassaolonPaattymispaiva'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

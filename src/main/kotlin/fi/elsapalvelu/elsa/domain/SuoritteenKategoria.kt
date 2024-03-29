package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "suoritteen_kategoria")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class SuoritteenKategoria(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "nimi_sv")
    var nimiSv: String? = null,

    @OneToMany(mappedBy = "kategoria")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var suoritteet: MutableSet<Suorite> = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    var erikoisala: Erikoisala? = null,

    @Column(name = "jarjestysnumero")
    var jarjestysnumero: Int? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritteenKategoria) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Suorite{" +
        "id=$id" +
        ", nimi='$nimi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "arvioitavan_kokonaisuuden_kategoria")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArvioitavanKokonaisuudenKategoria(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "nimi_sv")
    var nimiSv: String? = null,

    @Column(name = "jarjestysnumero")
    var jarjestysnumero: Int? = null,

    @NotNull
    @ManyToOne(optional = false)
    var erikoisala: Erikoisala? = null,

    @OneToMany(mappedBy = "kategoria")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var arvioitavatKokonaisuudet: MutableSet<ArvioitavaKokonaisuus> = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavanKokonaisuudenKategoria) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArvioitavanKokonaisuudenKategoria{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", jarjestysnumero=$jarjestysnumero" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

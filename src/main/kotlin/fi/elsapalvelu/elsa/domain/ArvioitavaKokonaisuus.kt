package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "arvioitava_kokonaisuus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArvioitavaKokonaisuus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "nimi_sv")
    var nimiSv: String? = null,

    @Column(name = "kuvaus")
    var kuvaus: String? = null,

    @Column(name = "kuvaus_sv")
    var kuvausSv: String? = null,

    @get: NotNull
    @Column(name = "voimassaolo_alkaa", nullable = false)
    var voimassaoloAlkaa: LocalDate? = null,

    @Column(name = "voimassaolo_loppuu")
    var voimassaoloLoppuu: LocalDate? = null,

    @NotNull
    @ManyToOne(optional = false)
    var kategoria: ArvioitavanKokonaisuudenKategoria? = null,

    @ManyToMany(mappedBy = "osaamistavoitteet")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var koulutusjaksot: MutableSet<Koulutusjakso>? = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaKokonaisuus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArvioitavaKokonaisuus{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", kuvaus='$kuvaus'" +
        ", voimassaoloAlkaa='$voimassaoloAlkaa'" +
        ", voimassaoloLoppuu='$voimassaoloLoppuu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

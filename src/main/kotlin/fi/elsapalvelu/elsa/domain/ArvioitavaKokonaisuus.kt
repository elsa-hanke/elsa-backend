package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "arvioitava_kokonaisuus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArvioitavaKokonaisuus(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "kuvaus")
    var kuvaus: String? = null,

    @get: NotNull
    @Column(name = "voimassaolo_alkaa", nullable = false)
    var voimassaoloAlkaa: LocalDate? = null,

    @Column(name = "voimassaolo_loppuu")
    var voimassaoloLoppuu: LocalDate? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["arvioitavatKokonaisuudet"], allowSetters = true)
    var erikoisala: Erikoisala? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["arvioitavatKokonaisuudet"], allowSetters = true)
    var kategoria: ArvioitavanKokonaisuudenKategoria? = null,

    @ManyToMany(mappedBy = "osaamistavoitteet")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(
        value = [
            "tyoskentelyjaksot", "osaamistavoitteet"
        ],
        allowSetters = true
    )
    var koulutusjaksot: MutableSet<Koulutusjakso>? = mutableSetOf()

) : Serializable {

    fun addKoulutusjakso(koulutusjakso: Koulutusjakso): ArvioitavaKokonaisuus {
        if (this.koulutusjaksot == null) {
            this.koulutusjaksot = mutableSetOf()
        }
        this.koulutusjaksot?.add(koulutusjakso)
        koulutusjakso.osaamistavoitteet?.add(this)
        return this
    }

    fun removeKoulutusjakso(koulutusjakso: Koulutusjakso): ArvioitavaKokonaisuus {
        this.koulutusjaksot?.remove(koulutusjakso)
        koulutusjakso.osaamistavoitteet?.remove(this)
        return this
    }

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

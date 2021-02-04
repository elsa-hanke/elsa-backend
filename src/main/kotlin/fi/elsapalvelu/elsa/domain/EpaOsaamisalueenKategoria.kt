package fi.elsapalvelu.elsa.domain

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

@Entity
@Table(name = "epa_osaamisalueen_kategoria")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class EpaOsaamisalueenKategoria(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @get: NotNull
    @Column(name = "jarjestysnumero", nullable = false)
    var jarjestysnumero: Int? = null,

    @get: NotNull
    @Column(name = "voimassaolo_alkaa", nullable = false)
    var voimassaoloAlkaa: LocalDate? = null,

    @Column(name = "voimassaolo_loppuu")
    var voimassaoloLoppuu: LocalDate? = null,

    @OneToMany(mappedBy = "kategoria")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var epaOsaamisalueet: MutableSet<EpaOsaamisalue> = mutableSetOf()

) : Serializable {

    fun addEpaOsaamisalue(epaOsaamisalue: EpaOsaamisalue): EpaOsaamisalueenKategoria {
        this.epaOsaamisalueet.add(epaOsaamisalue)
        epaOsaamisalue.kategoria = this
        return this
    }

    fun removeEpaOsaamisalue(epaOsaamisalue: EpaOsaamisalue): EpaOsaamisalueenKategoria {
        this.epaOsaamisalueet.remove(epaOsaamisalue)
        epaOsaamisalue.kategoria = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EpaOsaamisalueenKategoria) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "EpaOsaamisalueenKategoria{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", jarjestysnumero=$jarjestysnumero" +
        ", voimassaoloAlkaa='$voimassaoloAlkaa'" +
        ", voimassaoloLoppuu='$voimassaoloLoppuu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

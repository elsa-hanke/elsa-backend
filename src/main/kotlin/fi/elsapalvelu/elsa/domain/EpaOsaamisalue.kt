package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "epa_osaamisalue")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class EpaOsaamisalue(

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

    @OneToMany(mappedBy = "epaOsaamisalue")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var arvioitavatOsaalueet: MutableSet<ArvioitavaOsaalue> = mutableSetOf()

) : Serializable {

    fun addArvioitavaOsaalue(arvioitavaOsaalue: ArvioitavaOsaalue): EpaOsaamisalue {
        this.arvioitavatOsaalueet.add(arvioitavaOsaalue)
        arvioitavaOsaalue.epaOsaamisalue = this
        return this
    }

    fun removeArvioitavaOsaalue(arvioitavaOsaalue: ArvioitavaOsaalue): EpaOsaamisalue {
        this.arvioitavatOsaalueet.remove(arvioitavaOsaalue)
        arvioitavaOsaalue.epaOsaamisalue = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EpaOsaamisalue) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "EpaOsaamisalue{" +
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

package fi.elsapalvelu.elsa.domain

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

/**
 * A EpaOsaamisalue.
 */
@Entity
@Table(name = "epa_osaamisalue")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class EpaOsaamisalue(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "epa_tunnus", nullable = false)
    var epaTunnus: String? = null,

    @get: NotNull
    @Column(name = "epa_nimi", nullable = false)
    var epaNimi: String? = null,

    @Column(name = "kuvaus")
    var kuvaus: String? = null,

    @get: NotNull
    @Column(name = "voimassaolo_alkaa", nullable = false)
    var voimassaoloAlkaa: LocalDate? = null,

    @Column(name = "voimassaolo_loppuu")
    var voimassaoloLoppuu: LocalDate? = null,

    @OneToMany(mappedBy = "epaOsaamisalue")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var arvioitavaOsaalues: MutableSet<ArvioitavaOsaalue> = mutableSetOf()

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {

    fun addArvioitavaOsaalue(arvioitavaOsaalue: ArvioitavaOsaalue): EpaOsaamisalue {
        this.arvioitavaOsaalues.add(arvioitavaOsaalue)
        arvioitavaOsaalue.epaOsaamisalue = this
        return this
    }

    fun removeArvioitavaOsaalue(arvioitavaOsaalue: ArvioitavaOsaalue): EpaOsaamisalue {
        this.arvioitavaOsaalues.remove(arvioitavaOsaalue)
        arvioitavaOsaalue.epaOsaamisalue = null
        return this
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EpaOsaamisalue) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "EpaOsaamisalue{" +
        "id=$id" +
        ", epaTunnus='$epaTunnus'" +
        ", epaNimi='$epaNimi'" +
        ", kuvaus='$kuvaus'" +
        ", voimassaoloAlkaa='$voimassaoloAlkaa'" +
        ", voimassaoloLoppuu='$voimassaoloLoppuu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

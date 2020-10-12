package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*

/**
 * A Arviointiosaalue.
 */
@Entity
@Table(name = "arviointiosaalue")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Arviointiosaalue(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "aluetunnus")
    var aluetunnus: String? = null,

    @Column(name = "nimi")
    var nimi: String? = null,

    @Column(name = "kuvaus")
    var kuvaus: String? = null,

    @Column(name = "osaamisen_rajaarvo")
    var osaamisenRajaarvo: String? = null,

    @Column(name = "minimivaatimus")
    var minimivaatimus: String? = null,

    @Column(name = "voimassaolo_alkaa")
    var voimassaoloAlkaa: LocalDate? = null,

    @Column(name = "voimassaolo_loppuu")
    var voimassaoloLoppuu: LocalDate? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["arviointiosaalues"], allowSetters = true)
    var osaamisalueenArviointi: OsaamisalueenArviointi? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Arviointiosaalue) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Arviointiosaalue{" +
        "id=$id" +
        ", aluetunnus='$aluetunnus'" +
        ", nimi='$nimi'" +
        ", kuvaus='$kuvaus'" +
        ", osaamisenRajaarvo='$osaamisenRajaarvo'" +
        ", minimivaatimus='$minimivaatimus'" +
        ", voimassaoloAlkaa='$voimassaoloAlkaa'" +
        ", voimassaoloLoppuu='$voimassaoloLoppuu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

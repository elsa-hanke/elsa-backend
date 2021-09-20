package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.elsapalvelu.elsa.domain.enumeration.CanmedsOsaalue
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "arvioitava_osaalue")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArvioitavaOsaalue(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "tunnus", nullable = false)
    var tunnus: String? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "kuvaus")
    var kuvaus: String? = null,

    @Column(name = "osaamisen_rajaarvo")
    var osaamisenRajaarvo: String? = null,

    @Column(name = "arviointikriteerit")
    var arviointikriteerit: String? = null,

    @Column(name = "voimassaolo_alkaa")
    var voimassaoloAlkaa: LocalDate? = null,

    @Column(name = "voimassaolo_loppuu")
    var voimassaoloLoppuu: LocalDate? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rooli", nullable = false)
    var rooli: CanmedsOsaalue? = null,

    @ManyToOne
    @JsonIgnoreProperties(value = ["arvioitavaOsaalueet"], allowSetters = true)
    var arvioitavaKokonaisuus: ArvioitavaKokonaisuus? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArvioitavaOsaalue) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArvioitavaOsaalue{" +
        "id=$id" +
        ", tunnus='$tunnus'" +
        ", nimi='$nimi'" +
        ", kuvaus='$kuvaus'" +
        ", osaamisenRajaarvo='$osaamisenRajaarvo'" +
        ", arviointikriteerit='$arviointikriteerit'" +
        ", voimassaoloAlkaa='$voimassaoloAlkaa'" +
        ", voimassaoloLoppuu='$voimassaoloLoppuu'" +
        ", rooli='$rooli'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

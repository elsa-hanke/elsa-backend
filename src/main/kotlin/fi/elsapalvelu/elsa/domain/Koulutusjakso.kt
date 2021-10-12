package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "koulutusjakso")
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Koulutusjakso(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "muut_osaamistavoitteet")
    var muutOsaamistavoitteet: String? = null,

    @Column(name = "luotu")
    var luotu: LocalDate? = null,

    @Column(name = "tallennettu")
    var tallennettu: LocalDate? = null,

    @get: NotNull
    @Column(name = "lukittu", nullable = false)
    var lukittu: Boolean = false,

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "rel_koulutusjakso__tyoskentelyjakso",
        joinColumns = [
            JoinColumn(name = "koulutusjakso_id")
        ],
        inverseJoinColumns = [
            JoinColumn(name = "tyoskentelyjakso_id")
        ]
    )
    @JsonIgnoreProperties(
        value = [
            "tyoskentelypaikka",
            "suoritusarvioinnit",
            "suoritemerkinnat",
            "keskeytykset",
            "omaaErikoisalaaTukeva",
            "erikoistuvaLaakari",
            "koulutusjaksot"
        ],
        allowSetters = true
    )
    var tyoskentelyjaksot: MutableSet<Tyoskentelyjakso>? = mutableSetOf(),

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "rel_koulutusjakso__osaamistavoitteet",
        joinColumns = [
            JoinColumn(name = "koulutusjakso_id")
        ],
        inverseJoinColumns = [
            JoinColumn(name = "osaamistavoitteet_id")
        ]
    )
    @JsonIgnoreProperties(
        value = [
            "arvioitavatOsaalueet",
            "erikoisala",
            "kategoria",
            "koulutusjaksot"
        ],
        allowSetters = true
    )
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var osaamistavoitteet: MutableSet<ArvioitavaKokonaisuus>? = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(
        value = [
            "erikoistuvaLaakari",
            "koulutussuunnitelmaAsiakirja",
            "motivaatiokirjeAsiakirja",
            "koulutusjaksot"
        ],
        allowSetters = true
    )
    var koulutussuunnitelma: Koulutussuunnitelma? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Koulutusjakso) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Koulutusjakso{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", muutOsaamistavoitteet='$muutOsaamistavoitteet'" +
        ", luotu='$luotu'" +
        ", tallennettu='$tallennettu'" +
        ", lukittu='$lukittu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

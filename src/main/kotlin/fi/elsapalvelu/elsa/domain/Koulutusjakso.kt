package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "koulutusjakso")
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Koulutusjakso(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

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
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
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
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var osaamistavoitteet: MutableSet<ArvioitavaKokonaisuus>? = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
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

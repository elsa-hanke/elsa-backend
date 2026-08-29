package fi.elsapalvelu.elsa.domain.kayttaja

import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDateTime
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
@Entity
@Audited
@Table(name = "asiakirja")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SQLDelete(
    sql = """
        update asiakirja set poistettu = true, tyoskentelyjakso_id = null,
        teoriakoulutus_id = null, arviointi_id = null, itsearviointi_id = null,
        koejakson_vastuuhenkilon_arvio_id = null where id = ?
    """
)
@SQLRestriction("poistettu = false")
data class Asiakirja(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null,

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    var tyoskentelyjakso: Tyoskentelyjakso? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var teoriakoulutus: Teoriakoulutus? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var arviointi: Suoritusarviointi? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var itsearviointi: Suoritusarviointi? = null,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var koejaksonVastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio? = null,

    @NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @NotNull
    @Column(name = "tyyppi", nullable = false)
    var tyyppi: String? = null,

    @NotNull
    @Column(name = "lisattypvm", nullable = false)
    var lisattypvm: LocalDateTime? = null,

    @NotNull
    @Column(name = "poistettu", nullable = false)
    var poistettu: Boolean = false,

    @NotNull
    @OneToOne(
        optional = false,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE],
        fetch = FetchType.LAZY
    )
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var asiakirjaData: AsiakirjaData? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asiakirja) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Asiakirja{" +
        "id=$id" +
        ", nimi='$nimi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

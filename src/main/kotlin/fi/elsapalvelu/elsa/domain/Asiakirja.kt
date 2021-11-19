package fi.elsapalvelu.elsa.domain

import com.sun.istack.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Audited
@Table(name = "asiakirja")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Asiakirja(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @ManyToOne(optional = true)
    var tyoskentelyjakso: Tyoskentelyjakso? = null,

    @ManyToOne(optional = false)
    var teoriakoulutus: Teoriakoulutus? = null,

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
    @OneToOne(optional = false, cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
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

package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "terveyskeskuskoulutusjakson_hyvaksynta")
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class TerveyskeskuskoulutusjaksonHyvaksynta(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @Column(name = "virkailija_hyvaksynyt")
    var virkailijaHyvaksynyt: Boolean = false,

    @NotNull
    @Column(name = "vastuuhenkilo_hyvaksynyt")
    var vastuuhenkiloHyvaksynyt: Boolean = false,

    @Column(name = "korjausehdotus")
    var korjausehdotus: String? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TerveyskeskuskoulutusjaksonHyvaksynta) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "TerveyskeskusKoulutusjakso{" +
        "id=$id" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}


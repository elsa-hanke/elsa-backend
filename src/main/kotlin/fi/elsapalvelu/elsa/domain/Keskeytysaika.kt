package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "keskeytysaika")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Keskeytysaika(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "alkamispaiva", nullable = false)
    var alkamispaiva: LocalDate? = null,

    @get: NotNull
    @Column(name = "paattymispaiva", nullable = false)
    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    @get: Min(value = 0)
    @get: Max(value = 100)
    @Column(name = "poissaoloprosentti", nullable = false)
    var poissaoloprosentti: Int? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var poissaolonSyy: PoissaolonSyy? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var tyoskentelyjakso: Tyoskentelyjakso? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Keskeytysaika) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Keskeytysaika{" +
        "id=$id" +
        ", alkamispaiva='$alkamispaiva'" +
        ", paattymispaiva='$paattymispaiva'" +
        ", poissaoloprosentti=$poissaoloprosentti" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

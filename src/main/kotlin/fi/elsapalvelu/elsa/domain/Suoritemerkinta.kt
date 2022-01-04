package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
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
@Table(name = "suoritemerkinta")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Suoritemerkinta(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "suorituspaiva", nullable = false)
    var suorituspaiva: LocalDate? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "arviointiasteikon_taso", nullable = false)
    var arviointiasteikonTaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "vaativuustaso", nullable = false)
    var vaativuustaso: Int? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "lisatiedot")
    var lisatiedot: String? = null,

    @get: NotNull
    @Column(name = "lukittu", nullable = false)
    var lukittu: Boolean = false,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var suorite: Suorite? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var tyoskentelyjakso: Tyoskentelyjakso?  = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var arviointiasteikko: Arviointiasteikko? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Suoritemerkinta) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Suoritemerkinta{" +
        "id=$id" +
        ", suorituspaiva='$suorituspaiva'" +
        ", arviointiasteikonTaso=$arviointiasteikonTaso" +
        ", vaativuustaso=$vaativuustaso" +
        ", lisatiedot='$lisatiedot'" +
        ", lukittu='$lukittu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

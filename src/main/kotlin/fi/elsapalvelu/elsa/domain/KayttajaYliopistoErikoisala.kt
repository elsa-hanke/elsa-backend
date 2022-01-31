package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "kayttaja_yliopisto_erikoisala")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KayttajaYliopistoErikoisala(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var kayttaja: Kayttaja? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var yliopisto: Yliopisto? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var erikoisala: Erikoisala? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KayttajaYliopistoErikoisala) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "KayttajaYliopistoErikoisala{id=`${id}`}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

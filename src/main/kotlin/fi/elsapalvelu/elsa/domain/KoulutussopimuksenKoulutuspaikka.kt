package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import javax.persistence.*

@Entity
@Audited
@Table(name = "koulutussopimuksen_koulutuspaikka")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoulutussopimuksenKoulutuspaikka(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "yliopisto", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var yliopisto: String? = null,

    @ManyToOne
    @JsonIgnoreProperties(value = ["koulutussopimuksenKoulutuspaikat"], allowSetters = true)
    var koulutussopimus: KoejaksonKoulutussopimus? = null

) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KoulutussopimuksenKoulutuspaikka) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "KoulutussopimuksenKoulutuspaikka(" +
            "id=$id, " +
            "nimi=$nimi, " +
            "yliopisto=$yliopisto, " +
            "koulutussopimus=$koulutussopimus)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "tyoskentelypaikka")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Tyoskentelypaikka(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tyyppi", nullable = false)
    var tyyppi: TyoskentelyjaksoTyyppi? = null,

    @Column(name = "muu_tyyppi")
    var muuTyyppi: String? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var kunta: Kunta? = null,

    @OneToOne(mappedBy = "tyoskentelypaikka")
    var tyoskentelyjakso: Tyoskentelyjakso? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tyoskentelypaikka) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Tyoskentelypaikka{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", tyyppi='$tyyppi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

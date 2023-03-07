package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "kouluttajavaltuutus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Kouluttajavaltuutus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "alkamispaiva", nullable = false)
    var alkamispaiva: LocalDate? = null,

    @get: NotNull
    @Column(name = "paattymispaiva", nullable = false)
    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    @Column(name = "valtuutuksen_luontiaika", nullable = false)
    var valtuutuksenLuontiaika: Instant? = null,

    @get: NotNull
    @Column(name = "valtuutuksen_muokkausaika", nullable = false)
    var valtuutuksenMuokkausaika: Instant? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var valtuuttajaOpintooikeus: Opintooikeus? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var valtuutettu: Kayttaja? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Kouluttajavaltuutus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Kouluttajavaltuutus{" +
        "id=$id" +
        ", alkamispaiva='$alkamispaiva'" +
        ", paattymispaiva='$paattymispaiva'" +
        ", valtuutuksenLuontiaika='$valtuutuksenLuontiaika'" +
        ", valtuutuksenMuokkausaika='$valtuutuksenMuokkausaika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

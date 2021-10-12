package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "kouluttajavaltuutus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Kouluttajavaltuutus(

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
    @Column(name = "valtuutuksen_luontiaika", nullable = false)
    var valtuutuksenLuontiaika: Instant? = null,

    @get: NotNull
    @Column(name = "valtuutuksen_muokkausaika", nullable = false)
    var valtuutuksenMuokkausaika: Instant? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["annetutValtuutukset"], allowSetters = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var valtuuttaja: ErikoistuvaLaakari? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["saadutValtuutukset"], allowSetters = true)
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

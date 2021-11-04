package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "opiskeluoikeus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Opiskeluoikeus(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "opintooikeuden_myontamispaiva")
    var opintooikeudenMyontamispaiva: LocalDate? = null,

    @Column(name = "opintooikeuden_paattymispaiva")
    var opintooikeudenPaattymispaiva: LocalDate? = null,

    @Column(name = "opiskelijatunnus")
    var opiskelijatunnus: String? = null,

    @Column(name = "opintosuunnitelma_kaytossa_pvm")
    var opintosuunnitelmaKaytossaPvm: LocalDate? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @NotNull
    @ManyToOne(optional = false)
    var yliopisto: Yliopisto? = null,

    @NotNull
    @ManyToOne(optional = false)
    var erikoisala: Erikoisala? = null
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Opiskeluoikeus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Opiskeluoikeus{" +
        "id=$id" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}


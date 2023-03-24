package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "opintooikeus")
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Opintooikeus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "yliopisto_opintooikeus_id")
    var yliopistoOpintooikeusId: String? = null,

    @NotNull
    @Column(name = "opintooikeuden_myontamispaiva")
    var opintooikeudenMyontamispaiva: LocalDate? = null,

    @NotNull
    @Column(name = "opintooikeuden_paattymispaiva")
    var opintooikeudenPaattymispaiva: LocalDate? = null,

    @Column(name = "opiskelijatunnus")
    var opiskelijatunnus: String? = null,

    @NotNull
    @Column(name = "osaamisen_arvioinnin_oppaan_pvm")
    var osaamisenArvioinninOppaanPvm: LocalDate? = null,

    @NotNull
    @Column(name = "kaytossa")
    var kaytossa: Boolean = false,

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tila")
    var tila: OpintooikeudenTila? = null,

    @Column(name = "muokkausaika")
    var muokkausaika: Instant? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var yliopisto: Yliopisto? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var erikoisala: Erikoisala? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintoopas: Opintoopas? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var asetus: Asetus? = null,

    @OneToMany(
        mappedBy = "opintooikeus",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NotAudited
    var tyoskentelyjaksot: MutableSet<Tyoskentelyjakso> = mutableSetOf(),

    @OneToOne(
        fetch = FetchType.LAZY,
        mappedBy = "opintooikeus",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @NotAudited
    var koulutussuunnitelma: Koulutussuunnitelma? = null,

    @OneToMany(
        mappedBy = "opintooikeus",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NotAudited
    var teoriakoulutukset: MutableSet<Teoriakoulutus>? = mutableSetOf(),

    @OneToMany(
        mappedBy = "opintooikeus",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NotAudited
    var opintosuoritukset: MutableSet<Opintosuoritus>? = mutableSetOf(),

    @OneToMany(
        mappedBy = "valtuuttajaOpintooikeus",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NotAudited
    var annetutValtuutukset: MutableSet<Kouluttajavaltuutus> = mutableSetOf(),

    @NotNull
    @Column(name = "terveyskeskuskoulutusjakso_suoritettu")
    var terveyskoulutusjaksoSuoritettu: Boolean = false,

    @OneToOne(
        mappedBy = "opintooikeus",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @NotAudited
    var terveyskeskuskoulutusjaksonHyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta? = null,

    @NotNull
    @Column(name = "muokkausoikeudet_virkailijoilla")
    var muokkausoikeudetVirkailijoilla: Boolean = false

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Opintooikeus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Opintooikeus{" +
        "id=$id" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}


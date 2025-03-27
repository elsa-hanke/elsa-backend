package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ArvioinninPerustuminen
import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate

@Entity
@Audited
@Table(name = "suoritusarviointi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Suoritusarviointi(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "tapahtuman_ajankohta", nullable = false)
    var tapahtumanAjankohta: LocalDate? = null,

    @Column(name = "arvioitava_tapahtuma")
    var arvioitavaTapahtuma: String? = null,

    @get: NotNull
    @Column(name = "pyynnon_aika", nullable = false)
    var pyynnonAika: LocalDate? = null,

    @Column(name = "lisatiedot")
    var lisatiedot: String? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "itsearviointi_vaativuustaso")
    var itsearviointiVaativuustaso: Int? = null,

    @Column(name = "sanallinen_itsearviointi")
    var sanallinenItsearviointi: String? = null,

    @Column(name = "itsearviointi_aika")
    var itsearviointiAika: LocalDate? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "vaativuustaso")
    var vaativuustaso: Int? = null,

    @Column(name = "sanallinen_arviointi")
    var sanallinenArviointi: String? = null,

    @Column(name = "arviointi_aika")
    var arviointiAika: LocalDate? = null,

    @get: NotNull
    @Column(name = "lukittu", nullable = false)
    var lukittu: Boolean = false,

    @OneToMany(mappedBy = "suoritusarviointi", cascade = [CascadeType.ALL], orphanRemoval = true)
    @NotAudited
    var kommentit: MutableSet<SuoritusarvioinninKommentti> = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var arvioinninAntaja: Kayttaja? = null,

    @OneToMany(mappedBy = "suoritusarviointi", cascade = [CascadeType.ALL], orphanRemoval = true)
    @NotAudited
    var arvioitavatKokonaisuudet: MutableSet<SuoritusarvioinninArvioitavaKokonaisuus> = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var tyoskentelyjakso: Tyoskentelyjakso? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var arviointiasteikko: Arviointiasteikko? = null,

    @ManyToMany
    @JoinTable(
        name = "suoritusarvioinnin_arviointityokalut",
        joinColumns = [JoinColumn(name = "suoritusarviointi_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "arviointityokalu_id", referencedColumnName = "id")]
    )
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var arviointityokalut: MutableSet<Arviointityokalu>? = mutableSetOf(),

    @Enumerated(EnumType.STRING)
    @Column(name = "arviointi_perustuu")
    var arviointiPerustuu: ArvioinninPerustuminen? = null,

    @Column(name = "muu_peruste")
    var muuPeruste: String? = null,

    @OneToMany(
        mappedBy = "arviointi",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var arviointiAsiakirjat: MutableSet<Asiakirja> = mutableSetOf(),

    @OneToMany(
        mappedBy = "itsearviointi",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var itsearviointiAsiakirjat: MutableSet<Asiakirja> = mutableSetOf(),

    @OneToMany(
        mappedBy = "suoritusarviointi",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @NotAudited
    var arviointityokaluVastaukset: MutableSet<SuoritusarvioinninArviointityokalunVastaus> = mutableSetOf(),

    @get: NotNull
    @Column(name = "keskenerainen", nullable = false)
    var keskenerainen: Boolean = false,

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Suoritusarviointi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Suoritusarviointi{" +
        "id=$id" +
        ", tapahtumanAjankohta='$tapahtumanAjankohta'" +
        ", arvioitavaTapahtuma='$arvioitavaTapahtuma'" +
        ", pyynnonAika='$pyynnonAika'" +
        ", lisatiedot='$lisatiedot'" +
        ", itsearviointiVaativuustaso=$itsearviointiVaativuustaso" +
        ", sanallinenItsearviointi='$sanallinenItsearviointi'" +
        ", itsearviointiAika='$itsearviointiAika'" +
        ", vaativuustaso=$vaativuustaso" +
        ", sanallinenArviointi='$sanallinenArviointi'" +
        ", arviointiAika='$arviointiAika'" +
        ", lukittu='$lukittu'" +
        ", arviointiTyokalu='$arviointityokalut'" +
        ", arviointiPerustuu='$arviointiPerustuu'" +
        ", muuPeruste='$muuPeruste'" +
        ", keskenerainen='$keskenerainen'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

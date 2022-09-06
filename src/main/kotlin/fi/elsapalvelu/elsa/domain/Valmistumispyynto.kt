package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "valmistumispyynto")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Valmistumispyynto(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null,

    @Column(name = "selvitys_vanhentuneista_suorituksista")
    var selvitysVanhentuneistaSuorituksista: String? = null,

    @Column(name = "vastuuhenkilo_osaamisen_arvioija_kuittausaika")
    var vastuuhenkiloOsaamisenArvioijaKuittausaika: LocalDate? = null,

    @Column(name = "vastuuhenkilo_osaamisen_arvioija_palautusaika")
    var vastuuhenkiloOsaamisenArvioijaPalautusaika: LocalDate? = null,

    @Column(name = "vastuuhenkilo_osaamisen_arvioija_korjausehdotus")
    var vastuuhenkiloOsaamisenArvioijaKorjausehdotus: String? = null,

    @ManyToOne(optional = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var vastuuhenkiloOsaamisenArvioija: Kayttaja? = null,

    @Column(name = "virkailijan_kuittausaika")
    var virkailijanKuittausaika: LocalDate? = null,

    @Column(name = "virkailijan_palautusaika")
    var virkailijanPalautusaika: LocalDate? = null,

    @Column(name = "virkailijan_korjausehdotus")
    var virkailijanKorjausehdotus: String? = null,

    @ManyToOne(optional = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var virkailijaHyvaksyja: Kayttaja? = null,

    @Column(name = "virkailijan_saate")
    var virkailijanSaate: String? = null,

    @Column(name = "vastuuhenkilo_hyvaksyja_kuittausaika")
    var vastuuhenkiloHyvaksyjaKuittausaika: LocalDate? = null,

    @Column(name = "vastuuhenkilo_hyvaksyja_palautusaika")
    var vastuuhenkiloHyvaksyjaPalautusaika: LocalDate? = null,

    @Column(name = "vastuuhenkilo_hyvaksyja_korjausehdotus")
    var vastuuhenkiloHyvaksyjaKorjausehdotus: String? = null,

    @ManyToOne(optional = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var vastuuhenkiloHyvaksyja: Kayttaja? = null,

    @Column(name = "erikoistujan_kuittausaika", nullable = false)
    var erikoistujanKuittausaika: LocalDate? = null,

    @NotNull
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null,

    @Column(name = "sarakesign_request_id")
    var sarakeSignRequestId: String? = null,

    @Column(name = "allekirjoitusaika", nullable = false)
    var allekirjoitusaika: LocalDate? = null,

    ) : Serializable {

    @PrePersist
    protected fun onCreate() {
        muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
    }

    @PreUpdate
    protected fun onUpdate() {
        muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Valmistumispyynto) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Valmistumispyynto{" +
        "id=$id" +
        ", opintooikeus='$opintooikeus'" +
        ", vastuuhenkiloOsaamisenArvioijaKuittausaika='$vastuuhenkiloOsaamisenArvioijaKuittausaika'" +
        ", vastuuhenkiloOsaamisenArvioijaPalautusaika='$vastuuhenkiloOsaamisenArvioijaPalautusaika'" +
        ", vastuuhenkiloOsaamisenArvioijaKorjausehdotus='$vastuuhenkiloOsaamisenArvioijaKorjausehdotus'" +
        ", vastuuhenkiloOsaamisenArvioija='$vastuuhenkiloOsaamisenArvioija'" +
        ", virkailijanKuittausaika='$virkailijanKuittausaika'" +
        ", virkailijanPalautusaika='$virkailijanPalautusaika'" +
        ", virkailijanKorjausehdotus='$virkailijanKorjausehdotus'" +
        ", virkailijaHyvaksyja='$virkailijaHyvaksyja'" +
        ", virkailijanSaate='$virkailijanSaate'" +
        ", vastuuhenkiloHyvaksyjaKuittausaika='$vastuuhenkiloHyvaksyjaKuittausaika'" +
        ", vastuuhenkiloHyvaksyjaPalautusaika='$vastuuhenkiloHyvaksyjaPalautusaika'" +
        ", vastuuhenkiloHyvaksyjaKorjausehdotus='$vastuuhenkiloHyvaksyjaKorjausehdotus'" +
        ", vastuuhenkiloHyvaksyja='$vastuuhenkiloHyvaksyja'" +
        ", erikoistujanKuittausaika='$erikoistujanKuittausaika'" +
        ", muokkauspaiva='$muokkauspaiva'" +
        ", sarakeSignRequestId='$sarakeSignRequestId'" +
        ", allekirjoitusaika='$allekirjoitusaika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}


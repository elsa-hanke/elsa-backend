package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

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
    var virkailija: Kayttaja? = null,

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

    @OneToOne(
        mappedBy = "valmistumispyynto",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var valmistumispyynnonTarkistus: ValmistumispyynnonTarkistus? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(unique = true)
    var yhteenvetoAsiakirja: Asiakirja? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(unique = true)
    var liitteetAsiakirja: Asiakirja? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(unique = true)
    var erikoistujanTiedotAsiakirja: Asiakirja? = null

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
        ", virkailija='$virkailija'" +
        ", virkailijanSaate='$virkailijanSaate'" +
        ", vastuuhenkiloHyvaksyjaKuittausaika='$vastuuhenkiloHyvaksyjaKuittausaika'" +
        ", vastuuhenkiloHyvaksyjaPalautusaika='$vastuuhenkiloHyvaksyjaPalautusaika'" +
        ", vastuuhenkiloHyvaksyjaKorjausehdotus='$vastuuhenkiloHyvaksyjaKorjausehdotus'" +
        ", vastuuhenkiloHyvaksyja='$vastuuhenkiloHyvaksyja'" +
        ", erikoistujanKuittausaika='$erikoistujanKuittausaika'" +
        ", muokkauspaiva='$muokkauspaiva'" +
        "}"

    companion object {
        fun fromValmistumispyyntoErikoistuja(valmistumispyynto: Valmistumispyynto?): ValmistumispyynnonTila {
            return when {
                valmistumispyynto?.erikoistujanKuittausaika != null -> fromValmistumispyyntoNotReturned(valmistumispyynto)
                valmistumispyynto?.vastuuhenkiloOsaamisenArvioijaPalautusaika != null -> ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU
                valmistumispyynto?.virkailijanPalautusaika != null -> ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU
                valmistumispyynto?.vastuuhenkiloHyvaksyjaPalautusaika != null -> ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU
                else -> ValmistumispyynnonTila.UUSI
            }
        }

        fun fromValmistumispyyntoArvioija(
            valmistumispyynto: Valmistumispyynto,
            isAvoin: Boolean
        ): ValmistumispyynnonTila {
            return when {
                isAvoin || valmistumispyynto.erikoistujanKuittausaika != null -> fromValmistumispyyntoNotReturned(valmistumispyynto)
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika != null -> ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU
                valmistumispyynto.virkailijanPalautusaika != null -> ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU
                valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika != null -> ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU
                else -> fromValmistumispyyntoNotReturned(valmistumispyynto)
            }
        }

        fun fromValmistumispyyntoVirkailija(
            valmistumispyynto: Valmistumispyynto,
            isAvoin: Boolean
        ): ValmistumispyynnonTila {
            return when {
                isAvoin && valmistumispyynto.valmistumispyynnonTarkistus != null -> ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN
                isAvoin || valmistumispyynto.virkailijanKuittausaika != null -> fromValmistumispyyntoNotReturned(valmistumispyynto)
                valmistumispyynto.virkailijanPalautusaika != null -> ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU
                valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika != null -> ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU
                else -> fromValmistumispyyntoNotReturned(valmistumispyynto)
            }
        }

        fun fromValmistumispyyntoHyvaksyja(
            valmistumispyynto: Valmistumispyynto,
            isAvoin: Boolean
        ): ValmistumispyynnonTila {
            return when {
                isAvoin || valmistumispyynto.erikoistujanKuittausaika != null -> fromValmistumispyyntoNotReturned(valmistumispyynto)
                valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika != null -> ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU
                else -> fromValmistumispyyntoNotReturned(valmistumispyynto)
            }
        }

        fun fromValmistumispyyntoArvioijaHyvaksyja(
            valmistumispyynto: Valmistumispyynto,
            isAvoin: Boolean
        ): ValmistumispyynnonTila {
            return when {
                isAvoin || valmistumispyynto.erikoistujanKuittausaika != null -> fromValmistumispyyntoNotReturned(valmistumispyynto)
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaPalautusaika != null -> ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU
                valmistumispyynto.virkailijanPalautusaika != null -> ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU
                valmistumispyynto.vastuuhenkiloHyvaksyjaPalautusaika != null -> ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU
                else -> fromValmistumispyyntoNotReturned(valmistumispyynto)
            }
        }

        private fun fromValmistumispyyntoNotReturned(valmistumispyynto: Valmistumispyynto): ValmistumispyynnonTila {
            return when {
                valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika == null && valmistumispyynto.opintooikeus?.erikoisala?.id != YEK_ERIKOISALA_ID -> ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA
                valmistumispyynto.virkailijanKuittausaika == null -> ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA
                valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika == null -> ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
                else -> ValmistumispyynnonTila.HYVAKSYTTY
            }
        }
    }
}


package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "tyoskentelyjakso")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Tyoskentelyjakso(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "alkamispaiva", nullable = false)
    var alkamispaiva: LocalDate? = null,

    @Column(name = "paattymispaiva")
    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    @get: Min(value = 50)
    @get: Max(value = 100)
    @Column(name = "osaaikaprosentti", nullable = false)
    var osaaikaprosentti: Int? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kaytannon_koulutus", nullable = false)
    var kaytannonKoulutus: KaytannonKoulutusTyyppi? = null,

    @get: NotNull
    @Column(name = "hyvaksytty_aiempaan_erikoisalaan", nullable = false)
    var hyvaksyttyAiempaanErikoisalaan: Boolean = false,

    @NotNull
    @OneToOne(optional = false, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(unique = true)
    var tyoskentelypaikka: Tyoskentelypaikka? = null,

    @OneToMany(mappedBy = "tyoskentelyjakso")
    var suoritusarvioinnit: MutableSet<Suoritusarviointi> = mutableSetOf(),

    @OneToMany(mappedBy = "tyoskentelyjakso", cascade = [CascadeType.ALL], orphanRemoval = true)
    var suoritemerkinnat: MutableSet<Suoritemerkinta> = mutableSetOf(),

    @OneToMany(mappedBy = "tyoskentelyjakso", cascade = [CascadeType.ALL], orphanRemoval = true)
    var keskeytykset: MutableSet<Keskeytysaika> = mutableSetOf(),

    @ManyToOne
    @JsonIgnoreProperties(value = ["tyoskentelyjaksot"], allowSetters = true)
    var omaaErikoisalaaTukeva: Erikoisala? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["tyoskentelyjaksot"], allowSetters = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @Column(name = "liitetty_koejaksoon")
    var liitettyKoejaksoon: Boolean = false,

    ) : Serializable {

    fun addSuoritusarviointi(suoritusarviointi: Suoritusarviointi): Tyoskentelyjakso {
        this.suoritusarvioinnit.add(suoritusarviointi)
        suoritusarviointi.tyoskentelyjakso = this
        return this
    }

    fun removeSuoritusarviointi(suoritusarviointi: Suoritusarviointi): Tyoskentelyjakso {
        this.suoritusarvioinnit.remove(suoritusarviointi)
        suoritusarviointi.tyoskentelyjakso = null
        return this
    }

    fun addSuoritemerkinta(suoritemerkinta: Suoritemerkinta): Tyoskentelyjakso {
        this.suoritemerkinnat.add(suoritemerkinta)
        suoritemerkinta.tyoskentelyjakso = this
        return this
    }

    fun removeSuoritemerkinta(suoritemerkinta: Suoritemerkinta): Tyoskentelyjakso {
        this.suoritemerkinnat.remove(suoritemerkinta)
        suoritemerkinta.tyoskentelyjakso = null
        return this
    }

    fun addKeskeytysaika(keskeytysaika: Keskeytysaika): Tyoskentelyjakso {
        this.keskeytykset.add(keskeytysaika)
        keskeytysaika.tyoskentelyjakso = this
        return this
    }

    fun removeKeskeytysaika(keskeytysaika: Keskeytysaika): Tyoskentelyjakso {
        this.keskeytykset.remove(keskeytysaika)
        keskeytysaika.tyoskentelyjakso = null
        return this
    }

    fun isSuoritusarvioinnitNotEmpty(): Boolean {
        return this.suoritusarvioinnit.isNotEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tyoskentelyjakso) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Tyoskentelyjakso{" +
        "id=$id" +
        ", alkamispaiva='$alkamispaiva'" +
        ", paattymispaiva='$paattymispaiva'" +
        ", osaaikaprosentti=$osaaikaprosentti" +
        ", kaytannonKoulutus='$kaytannonKoulutus'" +
        ", hyvaksyttyAiempaanErikoisalaan='$hyvaksyttyAiempaanErikoisalaan'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

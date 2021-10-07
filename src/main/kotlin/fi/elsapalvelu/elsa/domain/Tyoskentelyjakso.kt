package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

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

    @OneToMany(mappedBy = "tyoskentelyjakso", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var asiakirjat: MutableSet<Asiakirja> = mutableSetOf()

) : Serializable {

    fun getMinPaattymispaiva(): LocalDate? {
        val dates = listOfNotNull(this.alkamispaiva)
            .plus(suoritemerkinnat.map { it.suorituspaiva })
            .plus(keskeytykset.map { it.paattymispaiva })
            .plus(suoritusarvioinnit.map { it.tapahtumanAjankohta })

        return dates.maxWithOrNull { o1, o2 ->
            o1!!.compareTo(o2 ?: LocalDate.MIN)
        }
    }

    fun hasTapahtumia(): Boolean {
        return suoritemerkinnat.size + keskeytykset.size + suoritusarvioinnit.size > 0
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

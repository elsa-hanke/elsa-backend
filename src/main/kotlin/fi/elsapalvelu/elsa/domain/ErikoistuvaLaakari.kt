package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Entity
@Table(name = "erikoistuva_laakari")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ErikoistuvaLaakari(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "syntymaaika")
    var syntymaaika: LocalDate? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    var kayttaja: Kayttaja? = null,

    @NotEmpty
    @OneToMany(mappedBy = "erikoistuvaLaakari")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var opintooikeudet: MutableSet<Opintooikeus> = mutableSetOf(),

    @OneToMany(mappedBy = "valtuuttaja")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var annetutValtuutukset: MutableSet<Kouluttajavaltuutus> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoistuvaLaakari")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var tyoskentelyjaksot: MutableSet<Tyoskentelyjakso> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoistuvaLaakari")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var teoriakoulutukset: MutableSet<Teoriakoulutus>? = mutableSetOf(),

    @NotNull
    @OneToOne(mappedBy = "erikoistuvaLaakari", cascade = [CascadeType.ALL], orphanRemoval = true)
    var koejaksonKoulutussopimus: KoejaksonKoulutussopimus? = null,

    @NotNull
    @OneToOne(mappedBy = "erikoistuvaLaakari", cascade = [CascadeType.ALL], orphanRemoval = true)
    var koejaksonAloituskeskustelu: KoejaksonAloituskeskustelu? = null,

    @OneToOne(mappedBy = "erikoistuvaLaakari")
    var koulutussuunnitelma: Koulutussuunnitelma? = null

) : Serializable {

    fun getYliopistoNimi(): String? {
        return this.opintooikeudet.firstOrNull()?.yliopisto?.nimi
    }

    fun getOpiskelijatunnus(): String? {
        return this.opintooikeudet.firstOrNull()?.opiskelijatunnus
    }

    fun getErikoisalaNimi(): String? {
        return this.opintooikeudet.firstOrNull()?.erikoisala?.nimi
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoistuvaLaakari) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ErikoistuvaLaakari{" +
        "id=$id" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

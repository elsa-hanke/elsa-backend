package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "erikoistuva_laakari")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ErikoistuvaLaakari(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "puhelinnumero", nullable = false)
    var puhelinnumero: String? = null,

    @Column(name = "opiskelijatunnus")
    var opiskelijatunnus: String? = null,

    @Column(name = "syntymaaika")
    var syntymaaika: LocalDate? = null,

    @Column(name = "opintosuunnitelma_kaytossa_pvm")
    var opintosuunnitelmaKaytossaPvm: LocalDate? = null,

    @Column(name = "opintooikeuden_myontamispaiva")
    var opintooikeudenMyontamispaiva: LocalDate? = null,

    @Column(name = "opintooikeuden_paattymispaiva")
    var opintooikeudenPaattymispaiva: LocalDate? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    var kayttaja: Kayttaja? = null,

    @OneToMany(mappedBy = "valtuuttaja")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var annetutValtuutukset: MutableSet<Kouluttajavaltuutus> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoistuvaLaakari")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var tyoskentelyjaksot: MutableSet<Tyoskentelyjakso> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoistuvaLaakari")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(
        value = [
            "erikoistuvaLaakari"
        ],
        allowSetters = true
    )
    var teoriakoulutukset: MutableSet<Teoriakoulutus>? = mutableSetOf(),

    // TODO: onko pakollinen tieto?
    // @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["erikoistuvatLaakarit"], allowSetters = true)
    var erikoisala: Erikoisala? = null,

    @NotNull
    @OneToOne(mappedBy = "erikoistuvaLaakari", cascade = [CascadeType.ALL], orphanRemoval = true)
    var koejaksonKoulutussopimus: KoejaksonKoulutussopimus? = null,

    @NotNull
    @OneToOne(mappedBy = "erikoistuvaLaakari", cascade = [CascadeType.ALL], orphanRemoval = true)
    var koejaksonAloituskeskustelu: KoejaksonAloituskeskustelu? = null,

    @JsonIgnoreProperties(
        value = [
            "erikoistuvaLaakari", "koulutussuunnitelmaAsiakirja", "motivaatiokirjeAsiakirja"
        ],
        allowSetters = true
    )
    @OneToOne(mappedBy = "erikoistuvaLaakari")
    var koulutussuunnitelma: Koulutussuunnitelma? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoistuvaLaakari) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ErikoistuvaLaakari{" +
        "id=$id" +
        ", puhelinnumero='$puhelinnumero'" +
        ", opiskelijatunnus='$opiskelijatunnus'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

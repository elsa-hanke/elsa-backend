package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "opintooikeus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Opintooikeus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

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
    @ManyToOne(optional = false)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @NotNull
    @ManyToOne(optional = false)
    var yliopisto: Yliopisto? = null,

    @NotNull
    @ManyToOne(optional = false)
    var erikoisala: Erikoisala? = null,

    @NotNull
    @ManyToOne(optional = false)
    var opintoopas: Opintoopas? = null,

    @NotNull
    @ManyToOne(optional = false)
    var asetus: Asetus? = null,

    @OneToMany(mappedBy = "opintooikeus")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var tyoskentelyjaksot: MutableSet<Tyoskentelyjakso> = mutableSetOf(),

    @OneToOne(mappedBy = "opintooikeus")
    var koulutussuunnitelma: Koulutussuunnitelma? = null,

    @OneToMany(mappedBy = "opintooikeus")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var teoriakoulutukset: MutableSet<Teoriakoulutus>? = mutableSetOf()

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


package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

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

    @Column(name = "sahkoposti", nullable = false)
    var sahkoposti: String? = null,

    @Column(name = "opiskelijatunnus")
    var opiskelijatunnus: String? = null,

    @get: Min(value = 1900)
    @get: Max(value = 2100)
    @Column(name = "opintojen_aloitusvuosi")
    var opintojenAloitusvuosi: Int? = null,

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

    // TODO: onko pakollinen tieto?
    // @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["erikoistuvatLaakarit"], allowSetters = true)
    var erikoisala: Erikoisala? = null,

    @OneToOne(mappedBy = "erikoistuvaLaakari")
    @JsonIgnore
    var koejakso: Koejakso? = null

) : Serializable {

    fun addValtuutus(kouluttajavaltuutus: Kouluttajavaltuutus): ErikoistuvaLaakari {
        this.annetutValtuutukset.add(kouluttajavaltuutus)
        kouluttajavaltuutus.valtuuttaja = this
        return this
    }

    fun removeValtuutus(kouluttajavaltuutus: Kouluttajavaltuutus): ErikoistuvaLaakari {
        this.annetutValtuutukset.remove(kouluttajavaltuutus)
        kouluttajavaltuutus.valtuuttaja = null
        return this
    }

    fun addTyoskentelyjakso(tyoskentelyjakso: Tyoskentelyjakso): ErikoistuvaLaakari {
        this.tyoskentelyjaksot.add(tyoskentelyjakso)
        tyoskentelyjakso.erikoistuvaLaakari = this
        return this
    }

    fun removeTyoskentelyjakso(tyoskentelyjakso: Tyoskentelyjakso): ErikoistuvaLaakari {
        this.tyoskentelyjaksot.remove(tyoskentelyjakso)
        tyoskentelyjakso.erikoistuvaLaakari = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoistuvaLaakari) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ErikoistuvaLaakari{" +
        "id=$id" +
        ", puhelinnumero='$puhelinnumero'" +
        ", sahkoposti='$sahkoposti'" +
        ", opiskelijatunnus='$opiskelijatunnus'" +
        ", opintojenAloitusvuosi=$opintojenAloitusvuosi" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

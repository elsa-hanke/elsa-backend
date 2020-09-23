package fi.oulu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

/**
 * A ErikoistuvaLaakari.
 */
@Entity
@Table(name = "erikoistuva_laakari")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ErikoistuvaLaakari(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,
    @get: NotNull
    @Column(name = "puhelinnumero", nullable = false)
    var puhelinnumero: String? = null,

    @get: NotNull
    @Column(name = "sahkoposti", nullable = false)
    var sahkoposti: String? = null,

    @Column(name = "opiskelijatunnus")
    var opiskelijatunnus: String? = null,

    @get: Min(value = 1900)
    @get: Max(value = 2100)
    @Column(name = "opintojen_aloitusvuosi")
    var opintojenAloitusvuosi: Int? = null,

    @OneToOne @JoinColumn(unique = true)
    var kayttaja: Kayttaja? = null,

    @OneToMany(mappedBy = "erikoistuvaLaakari")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var osoites: MutableSet<Osoite> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoistuvaLaakari")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var tyoskentelyjaksos: MutableSet<Tyoskentelyjakso> = mutableSetOf(),

    @OneToOne(mappedBy = "erikoistuvaLaakari")
    @JsonIgnore
    var hops: Hops? = null,

    @OneToOne(mappedBy = "erikoistuvaLaakari")
    @JsonIgnore
    var koejakso: Koejakso? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {

    fun addOsoite(osoite: Osoite): ErikoistuvaLaakari {
        this.osoites.add(osoite)
        osoite.erikoistuvaLaakari = this
        return this
    }

    fun removeOsoite(osoite: Osoite): ErikoistuvaLaakari {
        this.osoites.remove(osoite)
        osoite.erikoistuvaLaakari = null
        return this
    }

    fun addTyoskentelyjakso(tyoskentelyjakso: Tyoskentelyjakso): ErikoistuvaLaakari {
        this.tyoskentelyjaksos.add(tyoskentelyjakso)
        tyoskentelyjakso.erikoistuvaLaakari = this
        return this
    }

    fun removeTyoskentelyjakso(tyoskentelyjakso: Tyoskentelyjakso): ErikoistuvaLaakari {
        this.tyoskentelyjaksos.remove(tyoskentelyjakso)
        tyoskentelyjakso.erikoistuvaLaakari = null
        return this
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

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

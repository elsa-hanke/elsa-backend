package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.GenericGenerator
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.Email
import javax.validation.constraints.Size

@Entity
@Table(name = "kayttaja")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Kayttaja(

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    var id: String? = null,

    @field:Size(max = 50)
    @Column(name = "etunimi", length = 50)
    var etunimi: String? = null,

    @field:Size(max = 50)
    @Column(name = "sukunimi", length = 50)
    var sukunimi: String? = null,

    @Column(name = "nimike", nullable = true)
    var nimike: String? = null,

    @field:Email
    @field:Size(min = 5, max = 254)
    @Column(length = 254, unique = true)
    var sahkopostiosoite: String? = null,

    @Column
    var henkilotunnus: ByteArray? = null,

    // Henkiötunnuksen salausvektori
    @Column(name = "init_vector")
    var initVector: ByteArray? = null,

    // TODO: eduPersonPrincipalName

    @field:Size(max = 254)
    @Column(name = "puhelinnumero", length = 254)
    var puhelinnumero: String? = null,

    @Lob
    @Column(name = "profiilikuva")
    var profiilikuva: ByteArray? = null,

    @Column(name = "profiilikuva_content_type")
    var profiilikuvaContentType: String? = null,

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "kayttaja_authority",
        joinColumns = [JoinColumn(name = "kayttaja_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "authority_name", referencedColumnName = "name")]
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @BatchSize(size = 20)
    var authorities: MutableSet<Authority> = mutableSetOf(),


    @OneToMany(mappedBy = "valtuutettu")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var saadutValtuutukset: MutableSet<Kouluttajavaltuutus> = mutableSetOf(),

    @ManyToOne(optional = true)
    @JsonIgnoreProperties(value = ["kayttajat"], allowSetters = true)
    var yliopisto: Yliopisto? = null,

    ) : Serializable {

    fun addValtuutus(kouluttajavaltuutus: Kouluttajavaltuutus): Kayttaja {
        this.saadutValtuutukset.add(kouluttajavaltuutus)
        kouluttajavaltuutus.valtuutettu = this
        return this
    }

    fun removeValtuutus(kouluttajavaltuutus: Kouluttajavaltuutus): Kayttaja {
        this.saadutValtuutukset.remove(kouluttajavaltuutus)
        kouluttajavaltuutus.valtuutettu = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Kayttaja) return false
        if (other.id == null || id == null) return false

        return id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Kayttaja{" +
        "id=$id" +
        ", etunimi='" + etunimi + '\'' +
        ", sukunimi='" + sukunimi + '\'' +
        ", nimike='$nimike'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

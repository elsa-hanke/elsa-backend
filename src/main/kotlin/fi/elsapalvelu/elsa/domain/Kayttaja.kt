package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "kayttaja")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Kayttaja(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Lob
    @Column(name = "profiilikuva")
    var profiilikuva: ByteArray? = null,

    @Column(name = "profiilikuva_content_type")
    var profiilikuvaContentType: String? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    var user: User? = null,

    @OneToMany(mappedBy = "valtuutettu")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var saadutValtuutukset: MutableSet<Kouluttajavaltuutus> = mutableSetOf()

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

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Kayttaja{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", profiilikuva='?'" +
        ", profiilikuvaContentType='$profiilikuvaContentType'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.oulu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import fi.oulu.elsa.domain.enumeration.Kieli
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

/**
 * A Kayttaja.
 */
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

    @Enumerated(EnumType.STRING)
    @Column(name = "kieli")
    var kieli: Kieli? = null,

    @OneToOne @JoinColumn(unique = true)
    var user: User? = null,

    @ManyToMany(mappedBy = "keskustelijas")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnore
    var keskustelus: MutableSet<PikaviestiKeskustelu> = mutableSetOf()

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {

    fun addKeskustelu(pikaviestiKeskustelu: PikaviestiKeskustelu): Kayttaja {
        this.keskustelus.add(pikaviestiKeskustelu)
        pikaviestiKeskustelu.keskustelijas.add(this)
        return this
    }

    fun removeKeskustelu(pikaviestiKeskustelu: PikaviestiKeskustelu): Kayttaja {
        this.keskustelus.remove(pikaviestiKeskustelu)
        pikaviestiKeskustelu.keskustelijas.remove(this)
        return this
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Kayttaja) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Kayttaja{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", profiilikuva='$profiilikuva'" +
        ", profiilikuvaContentType='$profiilikuvaContentType'" +
        ", kieli='$kieli'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

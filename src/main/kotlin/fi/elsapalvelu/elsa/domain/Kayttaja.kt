package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import fi.elsapalvelu.elsa.domain.enumeration.Kieli
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
    @Enumerated(EnumType.STRING)
    @Column(name = "kieli")
    var kieli: Kieli? = null,

    @OneToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinColumn(unique = true)
    var user: User? = null,

    @ManyToMany(mappedBy = "keskustelijas")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnore
    var keskustelus: MutableSet<PikaviestiKeskustelu> = mutableSetOf()

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
        ", kieli='$kieli'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

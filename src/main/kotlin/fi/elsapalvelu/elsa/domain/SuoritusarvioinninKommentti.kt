package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
import java.io.Serializable
import java.time.Instant
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "suoritusarvioinnin_kommentti")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class SuoritusarvioinninKommentti(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "teksti", nullable = false)
    var teksti: String? = null,

    @get: NotNull
    @Column(name = "luontiaika", nullable = false)
    var luontiaika: Instant? = null,

    @get: NotNull
    @Column(name = "muokkausaika", nullable = false)
    var muokkausaika: Instant? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["suoritusarvioinninKommentit"], allowSetters = true)
    var kommentoija: Kayttaja? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["kommentit"], allowSetters = true)
    var suoritusarviointi: Suoritusarviointi? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarvioinninKommentti) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "SuoritusarvioinninKommentti{" +
        "id=$id" +
        ", teksti='$teksti'" +
        ", luontiaika='$luontiaika'" +
        ", muokkausaika='$muokkausaika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

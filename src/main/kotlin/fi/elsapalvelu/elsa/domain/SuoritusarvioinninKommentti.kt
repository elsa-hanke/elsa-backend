package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.Instant
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "suoritusarvioinnin_kommentti")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class SuoritusarvioinninKommentti(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var kommentoija: Kayttaja? = null,

    @NotNull
    @ManyToOne(optional = false)
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

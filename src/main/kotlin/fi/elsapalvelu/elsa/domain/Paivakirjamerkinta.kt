package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "paivakirjamerkinta")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Paivakirjamerkinta(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "paivamaara", nullable = false)
    var paivamaara: LocalDate? = null,

    @get: NotNull
    @Column(name = "oppimistapahtuman_nimi", nullable = false)
    var oppimistapahtumanNimi: String? = null,

    @Column(name = "muun_aiheen_nimi", nullable = false)
    var muunAiheenNimi: String? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "reflektio")
    var reflektio: String? = null,

    @get: NotNull
    @Column(name = "yksityinen", nullable = false)
    var yksityinen: Boolean = false,

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "rel_paivakirjamerkinta__aihekategoria",
        joinColumns = [
            JoinColumn(name = "paivakirjamerkinta_id")
        ],
        inverseJoinColumns = [
            JoinColumn(name = "aihekategoria_id")
        ]
    )
    var aihekategoriat: MutableSet<PaivakirjaAihekategoria>? = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @ManyToOne
    var teoriakoulutus: Teoriakoulutus? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Paivakirjamerkinta) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Paivakirjamerkinta{" +
        "id=$id" +
        ", paivamaara='$paivamaara'" +
        ", oppimistapahtumanNimi='$oppimistapahtumanNimi'" +
        ", muunAiheenNimi='$muunAiheenNimi'" +
        ", reflektio='$reflektio'" +
        ", yksityinen='$yksityinen'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

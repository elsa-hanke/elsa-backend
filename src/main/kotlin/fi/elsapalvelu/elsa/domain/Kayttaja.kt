package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "kayttaja")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Kayttaja(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "nimike", nullable = true)
    var nimike: String? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    var user: User? = null,

    @OneToMany(mappedBy = "valtuutettu")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var saadutValtuutukset: MutableSet<Kouluttajavaltuutus> = mutableSetOf(),

    @ManyToOne(optional = true)
    @JsonIgnoreProperties(value = ["kayttajat"], allowSetters = true)
    var yliopisto: Yliopisto? = null,

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "rel_kayttaja__yliopisto",
        joinColumns = [
            JoinColumn(name = "kayttaja_id")
        ],
        inverseJoinColumns = [
            JoinColumn(name = "yliopisto_id")
        ]
    )
    var yliopistot: MutableSet<Yliopisto>? = mutableSetOf(),

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "rel_kayttaja__erikoisala",
        joinColumns = [
            JoinColumn(name = "kayttaja_id")
        ],
        inverseJoinColumns = [
            JoinColumn(name = "erikoisala_id")
        ]
    )
    var erikoisalat: MutableSet<Erikoisala>? = mutableSetOf()

) : Serializable {

    fun getNimi(): String {
        return this.user?.firstName + " " + this.user?.lastName
    }

    fun getAvatar(): ByteArray? {
        return this.user?.avatar
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Kayttaja) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Kayttaja{" +
        "id=$id" +
        ", nimike='$nimike'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

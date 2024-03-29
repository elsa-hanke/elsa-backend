package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import java.io.Serializable
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "yliopisto")
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Yliopisto(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nimi", nullable = false)
    var nimi: YliopistoEnum? = null,

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "yliopisto_erikoisala",
        joinColumns = [JoinColumn(name = "yliopisto_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "erikoisala_id", referencedColumnName = "id")]
    )
    var erikoisalat: MutableSet<Erikoisala> = mutableSetOf(),

    @Column(name = "haka_id", nullable = true)
    var hakaId: String? = null,

    @Column(name = "haka_entity_id", nullable = true)
    var hakaEntityId: String? = null,

    @Column(name = "hae_opintotietodata", nullable = true)
    var haeOpintotietodata: Boolean? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Yliopisto) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Yliopisto{" +
        "id=$id" +
        ", nimi='$nimi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.Instant
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "opintooikeus_herate")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class OpintooikeusHerate(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @Column(name = "usea_voimassaoleva_herate_lahetetty")
    var useaVoimassaolevaHerateLahetetty: Instant? = null,

    @Column(name = "maaraaikainen_paattymassa_herate_lahetetty")
    var maaraaikainenPaattymassaHerateLahetetty: Instant? = null,

    @Column(name = "paattymassa_herate_lahetetty")
    var paattymassaHerateLahetetty: Instant? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintooikeusHerate) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "OpintooikeusHerate{" +
        "id=$id" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}


package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "erikoisala_sisu_tutkintoohjelma")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
class ErikoisalaSisuTutkintoohjelma (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "tutkintoohjelma_id", nullable = false)
    var tutkintoohjelmaId: String? = null,

    @ManyToOne(optional = false)
    var erikoisala: Erikoisala? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritteenKategoria) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ErikoisalaSisuTutkintoohjelma(id=$id)"

    companion object {
        private const val serialVersionUID = 1L
    }
}


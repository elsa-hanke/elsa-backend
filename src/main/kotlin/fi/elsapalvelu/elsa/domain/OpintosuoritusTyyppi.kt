package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "opintosuoritus_tyyppi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class OpintosuoritusTyyppi(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nimi", nullable = false)
    var nimi: OpintosuoritusTyyppiEnum? = null

) : Serializable
{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintosuoritusTyyppi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "OpintosuoritusTyyppi{id=$id, nimi=$nimi}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

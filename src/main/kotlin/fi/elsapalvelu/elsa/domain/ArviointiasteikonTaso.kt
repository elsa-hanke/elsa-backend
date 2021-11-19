package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikonTasoTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@Entity
@Table(name = "arviointiasteikon_taso")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArviointiasteikonTaso(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    var taso: Int? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nimi", nullable = false)
    var nimi: ArviointiasteikonTasoTyyppi? = null,

    @ManyToOne
    var arviointiasteikko: Arviointiasteikko? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointiasteikonTaso) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString(): String {
        return "ArviointiasteikonTaso(id=$id, taso=$taso, nimi=$nimi, arviointiasteikko=$arviointiasteikko)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

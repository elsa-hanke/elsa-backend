package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ArviointiasteikonTasoTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "arviointiasteikon_taso")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArviointiasteikonTaso(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

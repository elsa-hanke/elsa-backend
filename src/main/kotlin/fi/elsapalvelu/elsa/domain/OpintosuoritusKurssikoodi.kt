package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "opintosuoritus_kurssikoodi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class OpintosuoritusKurssikoodi(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "tunniste", nullable = false)
    var tunniste: String? = null,

    @get: NotNull
    @Column(name = "osakokonaisuus", nullable = false)
    var isOsakokonaisuus: Boolean? = null,

    @NotNull
    @ManyToOne(optional = false)
    var tyyppi: OpintosuoritusTyyppi? = null,

    @NotNull
    @ManyToOne(optional = false)
    var yliopisto: Yliopisto? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintosuoritusKurssikoodi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "OpintosuoritusKurssikoodi{id=$id" +
        ", tunniste=$tunniste" +
        ", isOsakokonaisuus=$isOsakokonaisuus"

    companion object {
        private const val serialVersionUID = 1L
    }
}


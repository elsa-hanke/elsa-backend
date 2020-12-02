package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "keskeytysaika")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Keskeytysaika(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "alkamispaiva", nullable = false)
    var alkamispaiva: LocalDate? = null,

    @get: NotNull
    @Column(name = "paattymispaiva", nullable = false)
    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    @get: Min(value = 0)
    @get: Max(value = 100)
    @Column(name = "osaaikaprosentti", nullable = false)
    var osaaikaprosentti: Int? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["keskeytykset"], allowSetters = true)
    var poissaolonSyy: PoissaolonSyy? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["keskeytykset"], allowSetters = true)
    var tyoskentelyjakso: Tyoskentelyjakso? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Keskeytysaika) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Keskeytysaika{" +
        "id=$id" +
        ", alkamispaiva='$alkamispaiva'" +
        ", paattymispaiva='$paattymispaiva'" +
        ", osaaikaprosentti=$osaaikaprosentti" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

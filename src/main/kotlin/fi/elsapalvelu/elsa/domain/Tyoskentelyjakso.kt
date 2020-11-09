package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "tyoskentelyjakso")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Tyoskentelyjakso(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "alkamispaiva", nullable = false)
    var alkamispaiva: LocalDate? = null,

    @Column(name = "paattymispaiva")
    var paattymispaiva: LocalDate? = null,

    @get: NotNull
    @get: Min(value = 50)
    @get: Max(value = 100)
    @Column(name = "osaaikaprosentti", nullable = false)
    var osaaikaprosentti: Int? = null,

    @NotNull
    @OneToOne(optional = false, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinColumn(unique = true)
    var tyoskentelypaikka: Tyoskentelypaikka? = null,

    @OneToMany(mappedBy = "tyoskentelyjakso")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var suoritusarvioinnit: MutableSet<Suoritusarviointi> = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["tyoskentelyjaksot"], allowSetters = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null

) : Serializable {

    fun addSuoritusarviointi(suoritusarviointi: Suoritusarviointi): Tyoskentelyjakso {
        this.suoritusarvioinnit.add(suoritusarviointi)
        suoritusarviointi.tyoskentelyjakso = this
        return this
    }

    fun removeSuoritusarviointi(suoritusarviointi: Suoritusarviointi): Tyoskentelyjakso {
        this.suoritusarvioinnit.remove(suoritusarviointi)
        suoritusarviointi.tyoskentelyjakso = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tyoskentelyjakso) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Tyoskentelyjakso{" +
        "id=$id" +
        ", alkamispaiva='$alkamispaiva'" +
        ", paattymispaiva='$paattymispaiva'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@Entity
@Table(name = "osaalueen_arviointi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class OsaalueenArviointi(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "arvio")
    var arvio: Int? = null,

    @ManyToOne
    @JsonIgnoreProperties(value = ["osaalueenArvioinnit"], allowSetters = true)
    var suoritusarviointi: Suoritusarviointi? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OsaalueenArviointi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "OsaalueenArviointi{" +
        "id=$id" +
        ", arvio=$arvio" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

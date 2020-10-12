package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

/**
 * A OsaalueenArviointi.
 */
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

    @ManyToOne @JsonIgnoreProperties(value = ["osaalueenArviointis"], allowSetters = true)
    var arvioitavaOsaalue: ArvioitavaOsaalue? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["osaalueenArviointis"], allowSetters = true)
    var suoritusarviointi: Suoritusarviointi? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

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

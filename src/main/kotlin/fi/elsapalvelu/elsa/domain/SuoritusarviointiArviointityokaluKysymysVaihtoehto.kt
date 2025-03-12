package fi.elsapalvelu.elsa.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable

@Entity
@Table(name = "suoritusarviointi_arviointityokalu_kysymys_vaihtoehto")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class SuoritusarviointiArviointityokaluKysymysVaihtoehto(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "teksti", nullable = false)
    var teksti: String? = null,

    @Column(name = "valittu", nullable = false)
    var valittu: Boolean? = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suoritusarviointi_arviointityokalu_kysymys_id", nullable = false)
    var arviointityokaluKysymys: SuoritusarviointiArviointityokaluKysymys? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiArviointityokaluKysymysVaihtoehto) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "SuoritusarviointiArviointityokaluKysymysVaihtoehto{" +
        "id=$id" +
        ", teksti='$teksti'" +
        ", valittu='$valittu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

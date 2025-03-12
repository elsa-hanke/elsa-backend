package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.service.dto.enumeration.ArviointityokaluKysymysTyyppi
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.NotAudited
import java.io.Serializable

@Entity
@Table(name = "suoritusarviointi_arviointityokalu_kysymys")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class SuoritusarviointiArviointityokaluKysymys(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "otsikko", nullable = false)
    var otsikko: String? = null,

    @Column(name = "pakollinen", nullable = false)
    var pakollinen: Boolean? = false,

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tyyppi")
    var tyyppi: ArviointityokaluKysymysTyyppi? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suoritusarviointi_arviointityokalu_id", nullable = false)
    var arviointityokalu: SuoritusarviointiArviointityokalu? = null,

    @OneToMany(
        mappedBy = "suoritusarviointiArviointityokaluKysymys",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NotAudited
    var vaihtoehdot: MutableList<SuoritusarviointiArviointityokaluKysymysVaihtoehto> = mutableListOf(),

    @Column(name = "jarjestysnumero")
    var jarjestysnumero: Int? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiArviointityokaluKysymys) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "SuoritusarviointiArviointityokaluKysymys{" +
        "id=$id" +
        ", otsikko='$otsikko'" +
        ", pakollinen='$pakollinen'" +
        ", tyyppi='$tyyppi'" +
        ", jarjestysnumero='$jarjestysnumero'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.service.dto.enumeration.ArviointityokaluKysymysTyyppi
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.NotAudited
import java.io.Serializable

@Entity
@Table(name = "arviointityokalu_kysymys")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArviointityokaluKysymys(

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
    @JoinColumn(name = "arviointityokalu_id", nullable = false)
    var arviointityokalu: Arviointityokalu? = null,

    @OneToMany(
        mappedBy = "arviointityokaluKysymys",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NotAudited
    var vaihtoehdot: MutableList<ArviointityokaluKysymysVaihtoehto> = mutableListOf(),

    @Column(name = "jarjestysnumero")
    var jarjestysnumero: Int? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointityokaluKysymys) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArviointityokaluKysymys{" +
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

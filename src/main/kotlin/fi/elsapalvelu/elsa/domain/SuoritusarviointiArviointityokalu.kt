package fi.elsapalvelu.elsa.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.NotAudited
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.io.Serializable
import java.time.Instant

@Entity
@Table(name = "suoritusarviointi_arviointityokalu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class SuoritusarviointiArviointityokalu(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "ohjeteksti", nullable = true)
    var ohjeteksti: String? = null,

    @ManyToOne(optional = true)
    var kategoria: ArviointityokaluKategoria? = null,

    @OneToMany(
        mappedBy = "suoritusarviointi_arviointityokalu",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NotAudited
    var kysymykset: MutableList<SuoritusarviointiArviointityokaluKysymys> = mutableListOf(),

    @CreatedDate
    @Column(name = "luontiaika", nullable = false)
    var luontiaika: Instant? = null,

    @LastModifiedDate
    @Column(name = "muokkausaika", nullable = false)
    var muokkausaika: Instant? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarviointiArviointityokalu) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "SuoritusarviointiArviointityokalu{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", ohjeteksti='$ohjeteksti'" +
        ", luontiaika='$luontiaika'" +
        ", muokkausaika='$muokkausaika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

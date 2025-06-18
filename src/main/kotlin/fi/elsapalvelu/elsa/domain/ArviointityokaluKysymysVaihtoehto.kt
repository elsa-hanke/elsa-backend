package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.service.dto.enumeration.ArviointityokaluKysymysVaihtoehtoTyyppi
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable

@Entity
@Table(name = "arviointityokalu_kysymys_vaihtoehto")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArviointityokaluKysymysVaihtoehto(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "teksti", nullable = false)
    var teksti: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arviointityokalu_kysymys_id", nullable = false)
    var arviointityokaluKysymys: ArviointityokaluKysymys? = null,

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tyyppi")
    var tyyppi: ArviointityokaluKysymysVaihtoehtoTyyppi? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointityokaluKysymysVaihtoehto) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArviointityokaluKysymysVaihtoehto{" +
        "id=$id" +
        ", teksti='$teksti'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

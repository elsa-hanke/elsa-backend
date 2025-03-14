package fi.elsapalvelu.elsa.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.io.Serializable
import java.time.Instant

@Entity
@Table(name = "arviointityokalu_kategoria")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ArviointityokaluKategoria(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false, length = 60)
    var nimi: String? = null,

    @CreatedDate
    @Column(name = "luontiaika", nullable = false)
    var luontiaika: Instant? = null,

    @LastModifiedDate
    @Column(name = "muokkausaika", nullable = false)
    var muokkausaika: Instant? = null,

    @NotNull
    @Column(name = "kaytossa")
    var kaytossa: Boolean = true,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointityokaluKategoria) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArviointityokaluKategoria{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", luontiaika='$luontiaika'" +
        ", muokkausaika='$muokkausaika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

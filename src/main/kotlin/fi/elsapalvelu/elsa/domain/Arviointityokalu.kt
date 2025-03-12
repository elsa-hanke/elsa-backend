package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ArviointityokalunTila
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
@Table(name = "arviointityokalu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Arviointityokalu(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "ohjeteksti", nullable = true)
    var ohjeteksti: String? = null,

    @NotNull
    @ManyToOne(optional = true)
    var kayttaja: Kayttaja? = null,

    @ManyToOne(optional = true)
    var kategoria: ArviointityokaluKategoria? = null,

    @OneToOne(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @JoinColumn(unique = true)
    var liite: AsiakirjaData? = null,

    @Column(name = "liitetiedoston_nimi")
    var liitetiedostonNimi: String? = null,

    @Column(name = "liitetiedoston_tyyppi")
    var liitetiedostonTyyppi: String? = null,

    @OneToMany(
        mappedBy = "arviointityokalu",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @NotAudited
    var kysymykset: MutableList<ArviointityokaluKysymys> = mutableListOf(),

    @NotNull
    @Column(name = "kaytossa")
    var kaytossa: Boolean = true,

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tila")
    var tila: ArviointityokalunTila? = null,

    @CreatedDate
    @Column(name = "luontiaika", nullable = false)
    var luontiaika: Instant? = null,

    @LastModifiedDate
    @Column(name = "muokkausaika", nullable = false)
    var muokkausaika: Instant? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Arviointityokalu) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Arviointityokalu{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", ohjeteksti='$ohjeteksti'" +
        ", kayttaja='$kayttaja'" +
        ", kaytossa='$kaytossa'" +
        ", luontiaika='$luontiaika'" +
        ", muokkausaika='$muokkausaika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

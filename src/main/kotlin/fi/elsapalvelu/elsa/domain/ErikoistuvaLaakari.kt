package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Entity
@Table(name = "erikoistuva_laakari")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ErikoistuvaLaakari(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "syntymaaika")
    var syntymaaika: LocalDate? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    var kayttaja: Kayttaja? = null,

    @NotEmpty
    @OneToMany(mappedBy = "erikoistuvaLaakari")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var opintooikeudet: MutableSet<Opintooikeus> = mutableSetOf()

) : Serializable {

    fun getYliopistoNimi(): String? {
        return this.getOpintooikeusKaytossa()?.yliopisto?.nimi.toString()
    }

    fun getYliopistoId(): Long? {
        return this.getOpintooikeusKaytossa()?.yliopisto?.id
    }

    fun getOpiskelijatunnus(): String? {
        return this.getOpintooikeusKaytossa()?.opiskelijatunnus
    }

    fun getErikoisalaNimi(): String? {
        return this.getOpintooikeusKaytossa()?.erikoisala?.nimi
    }

    fun getOpintooikeusKaytossa(): Opintooikeus? {
        return this.opintooikeudet.find { it.kaytossa } ?: throw EntityNotFoundException()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ErikoistuvaLaakari) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ErikoistuvaLaakari{" +
        "id=$id" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

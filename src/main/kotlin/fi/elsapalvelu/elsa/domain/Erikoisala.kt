package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "erikoisala")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Erikoisala(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tyyppi", nullable = false)
    var tyyppi: ErikoisalaTyyppi? = null,

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var kategoriat: MutableSet<SuoritteenKategoria> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var arvioitavatKokonaisuudet: MutableSet<ArvioitavaKokonaisuus> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoisala")
    var opintooppaat: MutableSet<Opintoopas> = mutableSetOf(),

    @ManyToMany(mappedBy = "erikoisalat")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var yliopistot: MutableSet<Yliopisto> = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Erikoisala) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Erikoisala{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", tyyppi='$tyyppi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

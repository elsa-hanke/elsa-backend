package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "erikoisala")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Erikoisala(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tyyppi", nullable = false)
    var tyyppi: ErikoisalaTyyppi? = null,

    @Column(name = "virta_patevyyskoodi", nullable = false)
    var virtaPatevyyskoodi: String? = null,

    @Column(name = "liittynyt_elsaan", nullable = true)
    var liittynytElsaan: Boolean? = null,

    @OneToMany(mappedBy = "erikoisala", cascade = [CascadeType.ALL], orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var sisuTutkintoohjelmat: MutableSet<ErikoisalaSisuTutkintoohjelma> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var suoritteenKategoriat: MutableSet<SuoritteenKategoria> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var arvioitavanKokonaisuudenKategoriat: MutableSet<ArvioitavanKokonaisuudenKategoria> = mutableSetOf(),

    @ManyToMany(mappedBy = "erikoisalat")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var yliopistot: MutableSet<Yliopisto> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var opintooppaat: MutableSet<Opintoopas> = mutableSetOf(),

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "rel_erikoisala_vastuuhenkilon_tehtavatyyppi",
        joinColumns = [
            JoinColumn(name = "erikoisala_id")
        ],
        inverseJoinColumns = [
            JoinColumn(name = "vastuuhenkilon_tehtavatyyppi_id")
        ]
    )
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var vastuuhenkilonTehtavatyypit: MutableSet<VastuuhenkilonTehtavatyyppi>? = mutableSetOf(),

    @Column(name = "siilo_koodi")
    var siiloKoodi: String? = null

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

package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
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
    @Column(name = "voimassaolo_alkaa", nullable = false)
    var voimassaoloAlkaa: LocalDate? = null,

    @Column(name = "voimassaolo_paattyy")
    var voimassaoloPaattyy: LocalDate? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tyyppi", nullable = false)
    var tyyppi: ErikoisalaTyyppi? = null,

    @get: NotNull
    @Column(name = "kaytannon_koulutuksen_vahimmaispituus", nullable = false)
    var kaytannonKoulutuksenVahimmaispituus: Double? = null,

    @get: NotNull
    @Column(name = "terveyskeskuskoulutusjakson_vahimmaispituus", nullable = false)
    var terveyskeskuskoulutusjaksonVahimmaispituus: Double? = null,

    @get: NotNull
    @Column(name = "yliopistosairaalajakson_vahimmaispituus", nullable = false)
    var yliopistosairaalajaksonVahimmaispituus: Double? = null,

    @get: NotNull
    @Column(name = "yliopistosairaalan_ulkopuolisen_tyoskentelyn_vahimmaispituus", nullable = false)
    var yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus: Double? = null,

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var kategoriat: MutableSet<OppimistavoitteenKategoria> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var arvioitavatKokonaisuudet: MutableSet<ArvioitavaKokonaisuus> = mutableSetOf(),

    @ManyToMany(mappedBy = "erikoisalat")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnore
    var yliopistot: MutableSet<Yliopisto> = mutableSetOf(),

    @ManyToOne
    @JsonIgnoreProperties(value = ["erikoisalat"], allowSetters = true)
    var arviointiasteikko: Arviointiasteikko? = null

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
        ", voimassaoloAlkaa='$voimassaoloAlkaa'" +
        ", voimassaoloPaattyy='$voimassaoloPaattyy'" +
        ", tyyppi='$tyyppi'" +
        ", kaytannonKoulutuksenVahimmaispituus=$kaytannonKoulutuksenVahimmaispituus" +
        ", terveyskeskuskoulutusjaksonVahimmaispituus=$terveyskeskuskoulutusjaksonVahimmaispituus" +
        ", yliopistosairaalajaksonVahimmaispituus=$yliopistosairaalajaksonVahimmaispituus" +
        ", yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus=$yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

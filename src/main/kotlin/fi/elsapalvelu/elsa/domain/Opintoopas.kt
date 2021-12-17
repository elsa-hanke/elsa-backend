package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "opintoopas")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Opintoopas(

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
    @Column(name = "kaytannon_koulutuksen_vahimmaispituus", nullable = false)
    var kaytannonKoulutuksenVahimmaispituus: Double? = null,

    @get: NotNull
    @Column(name = "terveyskeskuskoulutusjakson_vahimmaispituus", nullable = false)
    var terveyskeskuskoulutusjaksonVahimmaispituus: Double? = null,

    @Column(name = "terveyskeskuskoulutusjakson_maksimipituus", nullable = false)
    var terveyskeskuskoulutusjaksonMaksimipituus: Double? = null,

    @get: NotNull
    @Column(name = "yliopistosairaalajakson_vahimmaispituus", nullable = false)
    var yliopistosairaalajaksonVahimmaispituus: Double? = null,

    @get: NotNull
    @Column(name = "yliopistosairaalan_ulkopuolisen_tyoskentelyn_vahimmaispituus", nullable = false)
    var yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus: Double? = null,

    @get: NotNull
    @Column(name = "erikoisalan_vaatima_teoriakoulutusten_vahimmaismaara", nullable = false)
    var erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: Double? = null,

    @get: NotNull
    @Column(name = "erikoisalan_vaatima_sateilysuojakoulutusten_vahimmaismaara", nullable = false)
    var erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara: Double? = null,

    @get: NotNull
    @Column(name = "erikoisalan_vaatima_johtamisopintojen_vahimmaismaara", nullable = false)
    var erikoisalanVaatimaJohtamisopintojenVahimmaismaara: Double? = null,

    @ManyToOne
    var erikoisala: Erikoisala? = null,

    @ManyToOne
    var arviointiasteikko: Arviointiasteikko? = null,

    @OneToMany(mappedBy = "opintoopas")
    var opintooikeudet: MutableSet<Opintooikeus> = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Opintoopas) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Opintoopas{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", voimassaoloAlkaa='$voimassaoloAlkaa'" +
        ", voimassaoloPaattyy='$voimassaoloPaattyy'" +
        ", kaytannonKoulutuksenVahimmaispituus=$kaytannonKoulutuksenVahimmaispituus" +
        ", terveyskeskuskoulutusjaksonVahimmaispituus=$terveyskeskuskoulutusjaksonVahimmaispituus" +
        ", terveyskeskuskoulutusjaksonMaksimipituus=$terveyskeskuskoulutusjaksonMaksimipituus" +
        ", yliopistosairaalajaksonVahimmaispituus=$yliopistosairaalajaksonVahimmaispituus" +
        ", yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus=$yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus" +
        ", erikoisalanVaatimaTeoriakoulutustenVahimmaismaara=$erikoisalanVaatimaTeoriakoulutustenVahimmaismaara" +
        ", erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara=$erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara" +
        ", erikoisalanVaatimaJohtamisopintojenVahimmaismaara=$erikoisalanVaatimaJohtamisopintojenVahimmaismaara" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

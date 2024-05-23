package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.envers.RelationTargetAuditMode

@Entity
@Table(name = "opintoopas")
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Opintoopas(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "nimi_sv")
    var nimiSv: String? = null,

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

    @Column(name = "terveyskeskuskoulutusjakson_maksimipituus")
    var terveyskeskuskoulutusjaksonMaksimipituus: Double? = null,

    @get: NotNull
    @Column(name = "yliopistosairaalajakson_vahimmaispituus", nullable = false)
    var yliopistosairaalajaksonVahimmaispituus: Double? = null,

    @get: NotNull
    @Column(name = "yliopistosairaalan_ulkopuolisen_tyoskentelyn_vahimmaispituus", nullable = false)
    var yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus: Double? = null,

    @Column(name = "yliopistosairaalan_ulkopuolisen_tyoskentelyn_maksimipituus")
    var yliopistosairaalanUlkopuolisenTyoskentelynMaksimipituus: Double? = null,

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
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var arviointiasteikko: Arviointiasteikko? = null,

    @OneToMany(mappedBy = "opintoopas")
    @NotAudited
    var opintooikeudet: MutableSet<Opintooikeus> = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var erikoisala: Erikoisala? = null

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

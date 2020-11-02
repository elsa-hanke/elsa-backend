package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type

@Entity
@Table(name = "suoritusarviointi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Suoritusarviointi(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "tapahtuman_ajankohta")
    var tapahtumanAjankohta: LocalDate? = null,

    @Column(name = "arvioitava_tapahtuma")
    var arvioitavaTapahtuma: String? = null,

    @Column(name = "pyynnon_aika")
    var pyynnonAika: LocalDate? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "lisatiedot")
    var lisatiedot: String? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "itsearviointi_vaativuustaso")
    var itsearviointiVaativuustaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "itsearviointi_luottamuksen_taso")
    var itsearviointiLuottamuksenTaso: Int? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "sanallinen_itsearviointi")
    var sanallinenItsearviointi: String? = null,

    @Column(name = "itsearviointi_aika")
    var itsearviointiAika: LocalDate? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "vaativuustaso")
    var vaativuustaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "luottamuksen_taso")
    var luottamuksenTaso: Int? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "sanallinen_arviointi")
    var sanallinenArviointi: String? = null,

    @Column(name = "arviointi_aika")
    var arviointiAika: LocalDate? = null,

    @OneToMany(mappedBy = "suoritusarviointi")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var osaalueenArvioinnit: MutableSet<OsaalueenArviointi> = mutableSetOf(),

    @ManyToOne
    @JsonIgnoreProperties(value = ["suoritusarvioinnit"], allowSetters = true)
    var arvioinninAntaja: Kayttaja? = null,

    @ManyToOne
    @JsonIgnoreProperties(value = ["suoritusarvioinnit"], allowSetters = true)
    var arvioitavaOsaalue: EpaOsaamisalue? = null,

    @ManyToOne
    @JsonIgnoreProperties(value = ["suoritusarvioinnit"], allowSetters = true)
    var tyoskentelyjakso: Tyoskentelyjakso? = null

) : Serializable {

    fun addOsaalueenArviointi(osaalueenArviointi: OsaalueenArviointi): Suoritusarviointi {
        this.osaalueenArvioinnit.add(osaalueenArviointi)
        osaalueenArviointi.suoritusarviointi = this
        return this
    }

    fun removeOsaalueenArviointi(osaalueenArviointi: OsaalueenArviointi): Suoritusarviointi {
        this.osaalueenArvioinnit.remove(osaalueenArviointi)
        osaalueenArviointi.suoritusarviointi = null
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Suoritusarviointi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Suoritusarviointi{" +
        "id=$id" +
        ", tapahtumanAjankohta='$tapahtumanAjankohta'" +
        ", arvioitavaTapahtuma='$arvioitavaTapahtuma'" +
        ", pyynnonAika='$pyynnonAika'" +
        ", lisatiedot='$lisatiedot'" +
        ", itsearviointiVaativuustaso=$itsearviointiVaativuustaso" +
        ", itsearviointiLuottamuksenTaso=$itsearviointiLuottamuksenTaso" +
        ", sanallinenItsearviointi='$sanallinenItsearviointi'" +
        ", itsearviointiAika='$itsearviointiAika'" +
        ", vaativuustaso=$vaativuustaso" +
        ", luottamuksenTaso=$luottamuksenTaso" +
        ", sanallinenArviointi='$sanallinenArviointi'" +
        ", arviointiAika='$arviointiAika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

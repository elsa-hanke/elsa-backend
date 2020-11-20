package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "suoritusarviointi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Suoritusarviointi(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "tapahtuman_ajankohta", nullable = false)
    var tapahtumanAjankohta: LocalDate? = null,

    @get: NotNull
    @Column(name = "arvioitava_tapahtuma", nullable = false)
    var arvioitavaTapahtuma: String? = null,

    @get: NotNull
    @Column(name = "pyynnon_aika", nullable = false)
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

    @get: NotNull
    @Column(name = "lukittu", nullable = false)
    var lukittu: Boolean = false,

    @OneToMany(mappedBy = "suoritusarviointi")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var osaalueenArvioinnit: MutableSet<OsaalueenArviointi> = mutableSetOf(),

    @OneToMany(mappedBy = "suoritusarviointi", cascade = [CascadeType.ALL], orphanRemoval = true)
    var kommentit: MutableSet<SuoritusarvioinninKommentti> = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["suoritusarvioinnit"], allowSetters = true)
    var arvioinninAntaja: Kayttaja? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["suoritusarvioinnit"], allowSetters = true)
    var arvioitavaOsaalue: EpaOsaamisalue? = null,

    @NotNull
    @ManyToOne(optional = false)
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

    fun addKommentti(suoritusarvioinninKommentti: SuoritusarvioinninKommentti): Suoritusarviointi {
        this.kommentit.add(suoritusarvioinninKommentti)
        suoritusarvioinninKommentti.suoritusarviointi = this
        return this
    }

    fun removeKommentti(suoritusarvioinninKommentti: SuoritusarvioinninKommentti): Suoritusarviointi {
        this.kommentit.remove(suoritusarvioinninKommentti)
        suoritusarvioinninKommentti.suoritusarviointi = null
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
        ", lukittu='$lukittu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

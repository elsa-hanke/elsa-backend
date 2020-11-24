package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

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

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var kategoriat: MutableSet<OppimistavoitteenKategoria> = mutableSetOf(),

    @OneToMany(mappedBy = "erikoisala")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var epaOsaamisalueet: MutableSet<EpaOsaamisalue> = mutableSetOf(),

    @ManyToMany(mappedBy = "erikoisalat")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnore
    var yliopistot: MutableSet<Yliopisto> = mutableSetOf()

) : Serializable {

    fun addKategoria(oppimistavoitteenKategoria: OppimistavoitteenKategoria): Erikoisala {
        this.kategoriat.add(oppimistavoitteenKategoria)
        oppimistavoitteenKategoria.erikoisala = this
        return this
    }

    fun removeKategoria(oppimistavoitteenKategoria: OppimistavoitteenKategoria): Erikoisala {
        this.kategoriat.remove(oppimistavoitteenKategoria)
        oppimistavoitteenKategoria.erikoisala = null
        return this
    }

    fun addEpaOsaamisalue(epaOsaamisalue: EpaOsaamisalue): Erikoisala {
        this.epaOsaamisalueet.add(epaOsaamisalue)
        epaOsaamisalue.erikoisala = this
        return this
    }

    fun removeEpaOsaamisalue(epaOsaamisalue: EpaOsaamisalue): Erikoisala {
        this.epaOsaamisalueet.remove(epaOsaamisalue)
        epaOsaamisalue.erikoisala = null
        return this
    }

    fun addYliopisto(yliopisto: Yliopisto): Erikoisala {
        this.yliopistot.add(yliopisto)
        yliopisto.erikoisalat.add(this)
        return this
    }

    fun removeYliopisto(yliopisto: Yliopisto): Erikoisala {
        this.yliopistot.remove(yliopisto)
        yliopisto.erikoisalat.remove(this)
        return this
    }

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
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

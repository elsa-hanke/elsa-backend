package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "opintosuoritus_osakokonaisuus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class OpintosuoritusOsakokonaisuus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi_fi")
    var nimi_fi: String? = null,

    @Column(name = "nimi_sv")
    var nimi_sv: String? = null,

    @get: NotNull
    @Column(name = "kurssikoodi")
    var kurssikoodi: String? = null,

    @get: NotNull
    @Column(name = "suorituspaiva")
    var suorituspaiva: LocalDate? = null,

    @Column(name = "opintopisteet")
    var opintopisteet: Double? = null,

    @get: NotNull
    @Column(name = "hyvaksytty")
    var hyvaksytty: Boolean = false,

    @Column(name = "arvio_fi")
    var arvio_fi: String? = null,

    @Column(name = "arvio_sv")
    var arvio_sv: String? = null,

    @Column(name = "vanhenemispaiva")
    var vanhenemispaiva: LocalDate? = null,

    @Column(name = "muokkausaika")
    var muokkausaika: Instant? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintosuoritus: Opintosuoritus? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpintosuoritusOsakokonaisuus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "OpintosuoritusOsakokonaisuus{" +
        "id=$id" +
        ", nimi_fi='$nimi_fi'" +
        ", nimi_sv='$nimi_sv'" +
        ", suorituspaiva='$suorituspaiva'" +
        ", hyvaksytty='$hyvaksytty'" +
        ", arvio_fi='$arvio_fi'" +
        ", arvio_sv='${arvio_sv}v'" +
        ", vanhenemispaiva='$vanhenemispaiva'" +
        ", muokkausaika='$muokkausaika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}


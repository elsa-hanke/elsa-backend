package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "koejakson_loppukeskustelu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonLoppukeskustelu(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null,

    @Column(name = "esitetaan_koejakson_hyvaksymista")
    var esitetaanKoejaksonHyvaksymista: Boolean? = null,

    @Column(name = "jatkotoimenpiteet")
    var jatkotoimenpiteet: String? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var lahikouluttaja: Kayttaja? = null,

    @Column(name = "lahikouluttaja_hyvaksynyt")
    var lahikouluttajaHyvaksynyt: Boolean = false,

    @Column(name = "lahikouluttajan_kuittausaika")
    var lahikouluttajanKuittausaika: LocalDate? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var lahiesimies: Kayttaja? = null,

    @Column(name = "lahiesimies_hyvaksynyt")
    var lahiesimiesHyvaksynyt: Boolean = false,

    @Column(name = "lahiesimiehen_kuittausaika")
    var lahiesimiehenKuittausaika: LocalDate? = null,

    @NotNull
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null,

    @Column(name = "korjausehdotus")
    var korjausehdotus: String? = null,

    @Column(name = "erikoistuvan_kuittausaika", nullable = false)
    var erikoistuvanKuittausaika: LocalDate? = null

) : Serializable {

    @PrePersist
    protected fun onCreate() {
        muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
    }

    @PreUpdate
    protected fun onUpdate() {
        muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
    }

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KoejaksonLoppukeskustelu) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "KoejaksonLoppukeskustelu(" +
            "id=$id, " +
            "opintooikeus=$opintooikeus, " +
            "esitetaanKoejaksonHyvaksymista=$esitetaanKoejaksonHyvaksymista, " +
            "lahikouluttaja=$lahikouluttaja, " +
            "lahikouluttajaHyvaksynyt=$lahikouluttajaHyvaksynyt, " +
            "lahikouluttajanKuittausaika=$lahikouluttajanKuittausaika, " +
            "lahiesimies=$lahiesimies, " +
            "lahiesimiesHyvaksynyt=$lahiesimiesHyvaksynyt, " +
            "lahiesimiehenKuittausaika=$lahiesimiehenKuittausaika, " +
            "muokkauspaiva=$muokkauspaiva, " +
            "korjausehdotus=$korjausehdotus)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

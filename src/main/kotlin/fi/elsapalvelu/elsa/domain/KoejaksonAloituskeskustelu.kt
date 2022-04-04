package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "koejakson_aloituskeskustelu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonAloituskeskustelu(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null,

    @Column(name = "koejakson_suorituspaikka")
    var koejaksonSuorituspaikka: String? = null,

    @Column(name = "koejakson_toinen_suorituspaikka")
    var koejaksonToinenSuorituspaikka: String? = null,

    @Column(name = "koejakson_alkamispaiva")
    var koejaksonAlkamispaiva: LocalDate? = null,

    @Column(name = "koejakson_paattymispaiva")
    var koejaksonPaattymispaiva: LocalDate? = null,

    @Column(name = "suoritettu_kokoaikatyossa")
    var suoritettuKokoaikatyossa: Boolean = false,

    @Column(name = "tyotunnit_viikossa")
    var tyotunnitViikossa: Double? = null,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var lahikouluttaja: Kayttaja? = null,

    @Column(name = "lahikouluttaja_hyvaksynyt")
    var lahikouluttajaHyvaksynyt: Boolean = false,

    @Column(name = "lahikouluttajan_kuittausaika")
    var lahikouluttajanKuittausaika: LocalDate? = null,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var lahiesimies: Kayttaja? = null,

    @Column(name = "lahiesimies_hyvaksynyt")
    var lahiesimiesHyvaksynyt: Boolean = false,

    @Column(name = "lahiesimiehen_kuittausaika")
    var lahiesimiehenKuittausaika: LocalDate? = null,

    @Column(name = "koejakson_osaamistavoitteet")
    var koejaksonOsaamistavoitteet: String? = null,

    @NotNull
    @Column(name = "lahetetty", nullable = false)
    var lahetetty: Boolean = false,

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
        if (other !is KoejaksonAloituskeskustelu) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "Aloituskeskustelu(" +
            "id=$id, " +
            "opintooikeus=$opintooikeus, " +
            "koejaksonSuorituspaikka=$koejaksonSuorituspaikka, " +
            "koejaksonToinenSuorituspaikka=$koejaksonToinenSuorituspaikka, " +
            "koejaksonAlkamispaiva=$koejaksonAlkamispaiva, " +
            "koejaksonPaattymispaiva=$koejaksonPaattymispaiva, " +
            "suoritettuKokoaikatyossa=$suoritettuKokoaikatyossa, " +
            "tyotunnitViikossa=$tyotunnitViikossa, " +
            "lahikouluttaja=$lahikouluttaja, " +
            "lahikouluttajaHyvaksynyt=$lahikouluttajaHyvaksynyt, " +
            "lahikouluttajanKuittausaika=$lahikouluttajanKuittausaika, " +
            "lahiesimiesHyvaksynyt=$lahiesimiesHyvaksynyt, " +
            "lahiesimiehenKuittausaika=$lahiesimiehenKuittausaika, " +
            "koejaksonOsaamistavoitteet=$koejaksonOsaamistavoitteet, " +
            "lahetetty=$lahetetty, " +
            "muokkauspaiva=$muokkauspaiva, " +
            "korjausehdotus=$korjausehdotus)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

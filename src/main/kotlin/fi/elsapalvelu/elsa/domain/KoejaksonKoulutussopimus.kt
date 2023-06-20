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
@Table(name = "koejakson_koulutussopimus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonKoulutussopimus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null,

    @Column(name = "koejakson_alkamispaiva")
    var koejaksonAlkamispaiva: LocalDate? = null,

    @NotNull
    @Column(name = "lahetetty", nullable = false)
    var lahetetty: Boolean = false,

    @NotNull
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var vastuuhenkilo: Kayttaja? = null,

    @Column(name = "vastuuhenkilo_hyvaksynyt")
    var vastuuhenkiloHyvaksynyt: Boolean = false,

    @Column(name = "vastuuhenkilon_kuittausaika")
    var vastuuhenkilonKuittausaika: LocalDate? = null,

    @Column(name = "korjausehdotus")
    var korjausehdotus: String? = null,

    @Column(name = "vastuuhenkilon_korjausehdotus")
    var vastuuhenkilonKorjausehdotus: String? = null,

    @OneToMany(mappedBy = "koulutussopimus", cascade = [CascadeType.ALL], orphanRemoval = true)
    var kouluttajat: MutableSet<KoulutussopimuksenKouluttaja>? = mutableSetOf(),

    @OneToMany(mappedBy = "koulutussopimus", cascade = [CascadeType.ALL], orphanRemoval = true)
    var koulutuspaikat: MutableSet<KoulutussopimuksenKoulutuspaikka>? = mutableSetOf(),

    @Column(name = "erikoistuvan_allekirjoitusaika", nullable = false)
    var erikoistuvanAllekirjoitusaika: LocalDate? = null

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
        if (other !is KoejaksonKoulutussopimus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "KoejaksonKoulutussopimus(" +
            "id=$id, " +
            "opintooikeus=$opintooikeus, " +
            "erikoistuvanNimi=${opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()}, " +
            "erikoistuvanOpiskelijatunnus=${opintooikeus?.opiskelijatunnus}, " +
            "erikoistuvanSyntymaaika=${opintooikeus?.erikoistuvaLaakari?.syntymaaika}, " +
            "erikoistuvanYliopisto=${opintooikeus?.yliopisto?.nimi}, " +
            "opintooikeudenMyontamispaiva=${opintooikeus?.opintooikeudenMyontamispaiva}, " +
            "koejaksonAlkamispaiva=$koejaksonAlkamispaiva, " +
            "erikoistuvanPuhelinnumero=${opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.phoneNumber}, " +
            "erikoistuvanSahkoposti=${opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email}, " +
            "lahetetty=$lahetetty, " +
            "muokkauspaiva=$muokkauspaiva, " +
            "vastuuhenkilo=$vastuuhenkilo, " +
            "vastuuhenkiloHyvaksynyt=$vastuuhenkiloHyvaksynyt, " +
            "vastuuhenkilonKuittausaika=$vastuuhenkilonKuittausaika, " +
            "korjausehdotus=$korjausehdotus, " +
            "kouluttajat=$kouluttajat, " +
            "koulutuspaikat=$koulutuspaikat)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

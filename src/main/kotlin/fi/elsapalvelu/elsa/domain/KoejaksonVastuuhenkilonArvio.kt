package fi.elsapalvelu.elsa.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId

@Entity
@Audited
@Table(name = "koejakson_vastuuhenkilon_arvio")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonVastuuhenkilonArvio(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var vastuuhenkilo: Kayttaja? = null,

    @Column(name = "koejakso_hyvaksytty")
    var koejaksoHyvaksytty: Boolean? = null,

    @Column(name = "vastuuhenkilo_hyvaksynyt")
    var vastuuhenkiloHyvaksynyt: Boolean = false,

    @Column(name = "vastuuhenkilon_kuittausaika")
    var vastuuhenkilonKuittausaika: LocalDate? = null,

    @Column(name = "perustelu_hylkaamiselle")
    var perusteluHylkaamiselle: String? = null,

    @Column(name = "hylatty_arviointi_kayty_lapi_keskustellen")
    var hylattyArviointiKaytyLapiKeskustellen: Boolean? = null,

    @NotNull
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null,

    @Column(name = "erikoistuvan_kuittausaika", nullable = false)
    var erikoistuvanKuittausaika: LocalDate? = null,

    @ManyToOne
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var virkailija: Kayttaja? = null,

    @Column(name = "lisatiedot_virkailijalta")
    var lisatiedotVirkailijalta: String? = null,

    @Column(name = "virkailija_hyvaksynyt")
    var virkailijaHyvaksynyt: Boolean = false,

    @Column(name = "virkailijan_kuittausaika")
    var virkailijanKuittausaika: LocalDate? = null,

    @Column(name = "virkailijan_korjausehdotus")
    var virkailijanKorjausehdotus: String? = null,

    @Column(name = "vastuuhenkilon_korjausehdotus")
    var vastuuhenkilonKorjausehdotus: String? = null,

    @Column(name = "sarakesign_request_id")
    var sarakeSignRequestId: String? = null,

    @Column(name = "allekirjoitettu", nullable = false)
    var allekirjoitettu: Boolean = false,

    @OneToMany(
        mappedBy = "koejaksonVastuuhenkilonArvio",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var asiakirjat: MutableSet<Asiakirja> = mutableSetOf(),

    @Column(name = "allekirjoitusaika", nullable = false)
    var allekirjoitusaika: LocalDate? = null,

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
        if (other !is KoejaksonVastuuhenkilonArvio) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "KoejaksonVastuuhenkilonArvio(" +
            "id=$id, " +
            "opintooikeus=$opintooikeus, " +
            "vastuuhenkilo=$vastuuhenkilo, " +
            "vastuuhenkiloAllekirjoittanut=$vastuuhenkiloHyvaksynyt, " +
            "vastuuhenkilonKuittausaika=$vastuuhenkilonKuittausaika, " +
            "muokkauspaiva=$muokkauspaiva, " +
            "allekirjoitusaika='$allekirjoitusaika')"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

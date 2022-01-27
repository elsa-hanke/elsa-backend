package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "seurantajakso")
@EntityListeners(AuditingEntityListener::class)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Seurantajakso(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @Column(name = "alkamispaiva", nullable = false)
    var alkamispaiva: LocalDate? = null,

    @NotNull
    @Column(name = "paattymispaiva", nullable = false)
    var paattymispaiva: LocalDate? = null,

    @NotNull
    @Column(name = "oma_arviointi", nullable = false)
    var omaArviointi: String? = null,

    @Column(name = "lisahuomioita")
    var lisahuomioita: String? = null,

    @Column(name = "seuraavan_jakson_tavoitteet")
    var seuraavanJaksonTavoitteet: String? = null,

    @Column(name = "edistyminen_tavoitteiden_mukaista")
    var edistyminenTavoitteidenMukaista: Boolean? = null,

    @Column(name = "huolenaiheet")
    var huolenaiheet: String? = null,

    @Column(name = "kouluttajan_arvio")
    var kouluttajanArvio: String? = null,

    @Column(name = "erikoisalan_tyoskentelyvalmiudet")
    var erikoisalanTyoskentelyvalmiudet: String? = null,

    @Column(name = "jatkotoimet_ja_raportointi")
    var jatkotoimetJaRaportointi: String? = null,

    @Column(name = "hyvaksytty")
    var hyvaksytty: Boolean? = null,

    @Column(name = "seurantakeskustelun_yhteiset_merkinnat")
    var seurantakeskustelunYhteisetMerkinnat: String? = null,

    @Column(name = "seuraavan_keskustelun_ajankohta")
    var seuraavanKeskustelunAjankohta: LocalDate? = null,

    @Column(name = "korjausehdotus")
    var korjausehdotus: String? = null,

    @CreatedDate
    @Column(name = "luotu")
    var luotu: LocalDate? = null,

    @LastModifiedDate
    @Column(name = "tallennettu")
    var tallennettu: LocalDate? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var kouluttaja: Kayttaja? = null,

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JoinTable(
        name = "seurantajakso_koulutusjakso",
        joinColumns = [JoinColumn(name = "seurantajakso_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "koulutusjakso_id", referencedColumnName = "id")]
    )
    var koulutusjaksot: MutableSet<Koulutusjakso>? = mutableSetOf()

) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Seurantajakso) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "Seurantajakso(" +
            "id=$id, " +
            "alkamispaiva=$alkamispaiva, " +
            "paattymispaiva=$paattymispaiva, " +
            "omaArviointi=$omaArviointi, " +
            "lisahuomioita=$lisahuomioita, " +
            "seuraavanJaksonTavoitteet=$seuraavanJaksonTavoitteet, " +
            "edistyminenTavoitteidenMukaista=$edistyminenTavoitteidenMukaista, " +
            "huolenaiheet=$huolenaiheet, kouluttajanArvio=$kouluttajanArvio, " +
            "erikoisalanTyoskentelyvalmiudet=$erikoisalanTyoskentelyvalmiudet, " +
            "jatkotoimetJaRaportointi=$jatkotoimetJaRaportointi, " +
            "hyvaksytty=$hyvaksytty, " +
            "seurantakeskustelunYhteisetMerkinnat=$seurantakeskustelunYhteisetMerkinnat, " +
            "seuraavanKeskustelunAjankohta=$seuraavanKeskustelunAjankohta, " +
            "luotu=$luotu, " +
            "tallennettu=$tallennettu, " +
            "kouluttaja=$kouluttaja, " +
            "koulutusjaksot=$koulutusjaksot)"
    }


    companion object {
        private const val serialVersionUID = 1L
    }
}

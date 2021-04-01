package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "koejakson_koulutussopimus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonKoulutussopimus(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @NotNull
    @Column(name = "erikoistuvan_nimi", nullable = false)
    var erikoistuvanNimi: String? = null,

    @NotNull
    @Column(name = "erikoistuvan_opiskelijatunnus", nullable = false)
    var erikoistuvanOpiskelijatunnus: String? = null,

    @NotNull
    @Column(name = "erikoistuvan_syntymaaika", nullable = false)
    var erikoistuvanSyntymaaika: LocalDate? = null,

    @NotNull
    @Column(name = "erikoistuvan_yliopisto", nullable = false)
    var erikoistuvanYliopisto: String? = null,

    @NotNull
    @Column(name = "opintooikeuden_myontamispaiva", nullable = false)
    var opintooikeudenMyontamispaiva: LocalDate? = null,

    @NotNull
    @Column(name = "koejakson_alkamispaiva", nullable = false)
    var koejaksonAlkamispaiva: LocalDate? = null,

    @NotNull
    @Column(name = "erikoistuvan_puhelinnumero", nullable = false)
    var erikoistuvanPuhelinnumero: String? = null,

    @NotNull
    @Column(name = "erikoistuvan_sahkoposti", nullable = false)
    var erikoistuvanSahkoposti: String? = null,

    @NotNull
    @Column(name = "lahetetty", nullable = false)
    var lahetetty: Boolean = false,

    @NotNull
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null,

    @NotNull
    @ManyToOne(optional = false)
    var vastuuhenkilo: Kayttaja? = null,

    @NotNull
    @Column(name = "vastuuhenkilon_nimi", nullable = false)
    var vastuuhenkilonNimi: String? = null,

    @Column(name = "vastuuhenkilon_nimike")
    var vastuuhenkilonNimike: String? = null,

    @Column(name = "vastuuhenkilo_hyvaksynyt")
    var vastuuhenkiloHyvaksynyt: Boolean = false,

    @Column(name = "vastuuhenkilon_kuittausaika")
    var vastuuhenkilonKuittausaika: LocalDate? = null,

    @Column(name = "korjausehdotus")
    var korjausehdotus: String? = null,

    @OneToMany(mappedBy = "koulutussopimus", cascade = [CascadeType.ALL], orphanRemoval = true)
    var kouluttajat: MutableSet<KoulutussopimuksenKouluttaja> = mutableSetOf(),

    @OneToMany(mappedBy = "koulutussopimus", cascade = [CascadeType.ALL], orphanRemoval = true)
    var koulutuspaikat: MutableSet<KoulutussopimuksenKoulutuspaikka> = mutableSetOf()

) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KoejaksonKoulutussopimus

        if (id != other.id) return false
        if (erikoistuvaLaakari != other.erikoistuvaLaakari) return false
        if (erikoistuvanNimi != other.erikoistuvanNimi) return false
        if (erikoistuvanOpiskelijatunnus != other.erikoistuvanOpiskelijatunnus) return false
        if (erikoistuvanSyntymaaika != other.erikoistuvanSyntymaaika) return false
        if (erikoistuvanYliopisto != other.erikoistuvanYliopisto) return false
        if (opintooikeudenMyontamispaiva != other.opintooikeudenMyontamispaiva) return false
        if (koejaksonAlkamispaiva != other.koejaksonAlkamispaiva) return false
        if (erikoistuvanPuhelinnumero != other.erikoistuvanPuhelinnumero) return false
        if (erikoistuvanSahkoposti != other.erikoistuvanSahkoposti) return false
        if (lahetetty != other.lahetetty) return false
        if (muokkauspaiva != other.muokkauspaiva) return false
        if (vastuuhenkilo != other.vastuuhenkilo) return false
        if (vastuuhenkilonNimi != other.vastuuhenkilonNimi) return false
        if (vastuuhenkilonNimike != other.vastuuhenkilonNimike) return false
        if (vastuuhenkiloHyvaksynyt != other.vastuuhenkiloHyvaksynyt) return false
        if (vastuuhenkilonKuittausaika != other.vastuuhenkilonKuittausaika) return false
        if (korjausehdotus != other.korjausehdotus) return false
        if (kouluttajat != other.kouluttajat) return false
        if (koulutuspaikat != other.koulutuspaikat) return false

        return true
    }

    override fun toString(): String {
        return "KoejaksonKoulutussopimus(id=$id, erikoistuvaLaakari=$erikoistuvaLaakari, erikoistuvanNimi=$erikoistuvanNimi, erikoistuvanOpiskelijatunnus=$erikoistuvanOpiskelijatunnus, erikoistuvanSyntymaaika=$erikoistuvanSyntymaaika, erikoistuvanYliopisto=$erikoistuvanYliopisto, opintooikeudenMyontamispaiva=$opintooikeudenMyontamispaiva, koejaksonAlkamispaiva=$koejaksonAlkamispaiva, erikoistuvanPuhelinnumero=$erikoistuvanPuhelinnumero, erikoistuvanSahkoposti=$erikoistuvanSahkoposti, lahetetty=$lahetetty, muokkauspaiva=$muokkauspaiva, vastuuhenkilo=$vastuuhenkilo, vastuuhenkilonNimi=$vastuuhenkilonNimi, vastuuhenkilonNimike=$vastuuhenkilonNimike, vastuuhenkiloHyvaksynyt=$vastuuhenkiloHyvaksynyt, vastuuhenkilonKuittausaika=$vastuuhenkilonKuittausaika, korjausehdotus=$korjausehdotus, kouluttajat=$kouluttajat, koulutuspaikat=$koulutuspaikat)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

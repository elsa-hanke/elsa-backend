package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "koejakson_aloituskeskustelu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonAloituskeskustelu(

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
    @Column(name = "erikoistuvan_erikoisala", nullable = false)
    var erikoistuvanErikoisala: String? = null,

    @NotNull
    @Column(name = "erikoistuvan_opiskelijatunnus", nullable = false)
    var erikoistuvanOpiskelijatunnus: String? = null,

    @NotNull
    @Column(name = "erikoistuvan_yliopisto", nullable = false)
    var erikoistuvanYliopisto: String? = null,

    @NotNull
    @Column(name = "erikoistuvan_sahkoposti", nullable = false)
    var erikoistuvanSahkoposti: String? = null,

    @NotNull
    @Column(name = "koejakson_suorituspaikka", nullable = false)
    var koejaksonSuorituspaikka: String? = null,

    @Column(name = "koejakson_toinen_suorituspaikka")
    var koejaksonToinenSuorituspaikka: String? = null,

    @NotNull
    @Column(name = "koejakson_alkamispaiva", nullable = false)
    var koejaksonAlkamispaiva: LocalDate? = null,

    @NotNull
    @Column(name = "koejakson_paattymispaiva", nullable = false)
    var koejaksonPaattymispaiva: LocalDate? = null,

    @NotNull
    @Column(name = "suoritettu_kokoaikatyossa", nullable = false)
    var suoritettuKokoaikatyossa: Boolean = false,

    @Column(name = "tyotunnit_viikossa")
    var tyotunnitViikossa: Double? = null,

    @NotNull
    @ManyToOne(optional = false)
    var lahikouluttaja: Kayttaja? = null,

    @NotNull
    @Column(name = "lahikouluttajan_nimi", nullable = false)
    var lahikouluttajanNimi: String? = null,

    @Column(name = "lahikouluttaja_hyvaksynyt")
    var lahikouluttajaHyvaksynyt: Boolean = false,

    @Column(name = "lahikouluttajan_kuittausaika")
    var lahikouluttajanKuittausaika: LocalDate? = null,

    @NotNull
    @ManyToOne(optional = false)
    var lahiesimies: Kayttaja? = null,

    @NotNull
    @Column(name = "lahiesimiehen_nimi", nullable = false)
    var lahiesimiehenNimi: String? = null,

    @Column(name = "lahiesimies_hyvaksynyt")
    var lahiesimiesHyvaksynyt: Boolean = false,

    @Column(name = "lahiesimiehen_kuittausaika")
    var lahiesimiehenKuittausaika: LocalDate? = null,

    @NotNull
    @Column(name = "koejakson_osaamistavoitteet", nullable = false)
    var koejaksonOsaamistavoitteet: String? = null,

    @NotNull
    @Column(name = "lahetetty", nullable = false)
    var lahetetty: Boolean = false,

    @NotNull
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null,

    @Column(name = "korjausehdotus")
    var korjausehdotus: String? = null

) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KoejaksonAloituskeskustelu

        if (id != other.id) return false
        if (erikoistuvaLaakari != other.erikoistuvaLaakari) return false
        if (erikoistuvanNimi != other.erikoistuvanNimi) return false
        if (erikoistuvanErikoisala != other.erikoistuvanErikoisala) return false
        if (erikoistuvanOpiskelijatunnus != other.erikoistuvanOpiskelijatunnus) return false
        if (erikoistuvanYliopisto != other.erikoistuvanYliopisto) return false
        if (erikoistuvanSahkoposti != other.erikoistuvanSahkoposti) return false
        if (koejaksonSuorituspaikka != other.koejaksonSuorituspaikka) return false
        if (koejaksonToinenSuorituspaikka != other.koejaksonToinenSuorituspaikka) return false
        if (koejaksonAlkamispaiva != other.koejaksonAlkamispaiva) return false
        if (koejaksonPaattymispaiva != other.koejaksonPaattymispaiva) return false
        if (suoritettuKokoaikatyossa != other.suoritettuKokoaikatyossa) return false
        if (tyotunnitViikossa != other.tyotunnitViikossa) return false
        if (lahikouluttaja != other.lahikouluttaja) return false
        if (lahikouluttajanNimi != other.lahikouluttajanNimi) return false
        if (lahikouluttajaHyvaksynyt != other.lahikouluttajaHyvaksynyt) return false
        if (lahikouluttajanKuittausaika != other.lahikouluttajanKuittausaika) return false
        if (lahiesimies != other.lahiesimies) return false
        if (lahiesimiehenNimi != other.lahiesimiehenNimi) return false
        if (lahiesimiesHyvaksynyt != other.lahiesimiesHyvaksynyt) return false
        if (lahiesimiehenKuittausaika != other.lahiesimiehenKuittausaika) return false
        if (koejaksonOsaamistavoitteet != other.koejaksonOsaamistavoitteet) return false
        if (lahetetty != other.lahetetty) return false
        if (muokkauspaiva != other.muokkauspaiva) return false
        if (korjausehdotus != other.korjausehdotus) return false

        return true
    }

    override fun toString(): String {
        return "Aloituskeskustelu(id=$id, erikoistuvaLaakari=$erikoistuvaLaakari, erikoistuvanNimi=$erikoistuvanNimi, erikoistuvanErikoisala=$erikoistuvanErikoisala, erikoistuvanOpiskelijatunnus=$erikoistuvanOpiskelijatunnus, erikoistuvanYliopisto=$erikoistuvanYliopisto, erikoistuvanSahkoposti=$erikoistuvanSahkoposti, koejaksonSuorituspaikka=$koejaksonSuorituspaikka, koejaksonToinenSuorituspaikka=$koejaksonToinenSuorituspaikka, koejaksonAlkamispaiva=$koejaksonAlkamispaiva, koejaksonPaattymispaiva=$koejaksonPaattymispaiva, suoritettuKokoaikatyossa=$suoritettuKokoaikatyossa, tyotunnitViikossa=$tyotunnitViikossa, lahikouluttaja=$lahikouluttaja, lahikouluttajanNimi=$lahikouluttajanNimi, lahikouluttajaHyvaksynyt=$lahikouluttajaHyvaksynyt, lahikouluttajanKuittausaika=$lahikouluttajanKuittausaika, lahiesimies=$lahiesimies, lahiesimiehenNimi=$lahiesimiehenNimi, lahiesimiesHyvaksynyt=$lahiesimiesHyvaksynyt, lahiesimiehenKuittausaika=$lahiesimiehenKuittausaika, koejaksonOsaamistavoitteet=$koejaksonOsaamistavoitteet, lahetetty=$lahetetty, muokkauspaiva=$muokkauspaiva, korjausehdotus=$korjausehdotus)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

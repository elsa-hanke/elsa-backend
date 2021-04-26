package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "koejakson_loppukeskustelu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonLoppukeskustelu(

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
    @Column(name = "esitetaan_koejakson_hyvaksymista", nullable = false)
    var esitetaanKoejaksonHyvaksymista: Boolean = false,

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
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null,

    @Column(name = "korjausehdotus")
    var korjausehdotus: String? = null,

    @Column(name = "erikoistuva_allekirjoittanut")
    var erikoistuvaAllekirjoittanut: Boolean = false

) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KoejaksonLoppukeskustelu

        if (id != other.id) return false
        if (erikoistuvaLaakari != other.erikoistuvaLaakari) return false
        if (erikoistuvanNimi != other.erikoistuvanNimi) return false
        if (erikoistuvanErikoisala != other.erikoistuvanErikoisala) return false
        if (erikoistuvanOpiskelijatunnus != other.erikoistuvanOpiskelijatunnus) return false
        if (erikoistuvanYliopisto != other.erikoistuvanYliopisto) return false
        if (esitetaanKoejaksonHyvaksymista != other.esitetaanKoejaksonHyvaksymista) return false
        if (lahikouluttaja != other.lahikouluttaja) return false
        if (lahikouluttajanNimi != other.lahikouluttajanNimi) return false
        if (lahikouluttajaHyvaksynyt != other.lahikouluttajaHyvaksynyt) return false
        if (lahikouluttajanKuittausaika != other.lahikouluttajanKuittausaika) return false
        if (lahiesimies != other.lahiesimies) return false
        if (lahiesimiehenNimi != other.lahiesimiehenNimi) return false
        if (lahiesimiesHyvaksynyt != other.lahiesimiesHyvaksynyt) return false
        if (lahiesimiehenKuittausaika != other.lahiesimiehenKuittausaika) return false
        if (muokkauspaiva != other.muokkauspaiva) return false
        if (korjausehdotus != other.korjausehdotus) return false
        if (erikoistuvaAllekirjoittanut != other.erikoistuvaAllekirjoittanut) return false

        return true
    }

    override fun toString(): String {
        return "KoejaksonLoppukeskustelu(id=$id, erikoistuvaLaakari=$erikoistuvaLaakari, erikoistuvanNimi=$erikoistuvanNimi, erikoistuvanErikoisala=$erikoistuvanErikoisala, erikoistuvanOpiskelijatunnus=$erikoistuvanOpiskelijatunnus, erikoistuvanYliopisto=$erikoistuvanYliopisto, esitetaanKoejaksonHyvaksymista=$esitetaanKoejaksonHyvaksymista, lahikouluttaja=$lahikouluttaja, lahikouluttajanNimi=$lahikouluttajanNimi, lahikouluttajaHyvaksynyt=$lahikouluttajaHyvaksynyt, lahikouluttajanKuittausaika=$lahikouluttajanKuittausaika, lahiesimies=$lahiesimies, lahiesimiehenNimi=$lahiesimiehenNimi, lahiesimiesHyvaksynyt=$lahiesimiesHyvaksynyt, lahiesimiehenKuittausaika=$lahiesimiehenKuittausaika, muokkauspaiva=$muokkauspaiva, korjausehdotus=$korjausehdotus, erikoistuvaAllekirjoittanut=$erikoistuvaAllekirjoittanut)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

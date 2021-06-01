package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "koejakson_valiarviointi")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonValiarviointi(

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
    @Column(name = "edistyminen_tavoitteiden_mukaista", nullable = false)
    var edistyminenTavoitteidenMukaista: Boolean = false,

    @Column(name = "vahvuudet")
    var vahvuudet: String? = null,

    @Column(name = "kehittamistoimenpiteet")
    var kehittamistoimenpiteet: String? = null,

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
        if (other !is KoejaksonValiarviointi) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "KoejaksonValiarviointi(" +
            "id=$id, " +
            "erikoistuvaLaakari=$erikoistuvaLaakari, " +
            "erikoistuvanNimi=$erikoistuvanNimi, " +
            "erikoistuvanErikoisala=$erikoistuvanErikoisala, " +
            "erikoistuvanOpiskelijatunnus=$erikoistuvanOpiskelijatunnus, " +
            "erikoistuvanYliopisto=$erikoistuvanYliopisto, " +
            "edistyminenTavoitteidenMukaista=$edistyminenTavoitteidenMukaista, " +
            "vahvuudet=$vahvuudet, " +
            "kehittamistoimenpiteet=$kehittamistoimenpiteet, " +
            "lahikouluttaja=$lahikouluttaja, " +
            "lahikouluttajanNimi=$lahikouluttajanNimi, " +
            "lahikouluttajaHyvaksynyt=$lahikouluttajaHyvaksynyt, " +
            "lahikouluttajanKuittausaika=$lahikouluttajanKuittausaika, " +
            "lahiesimies=$lahiesimies, " +
            "lahiesimiehenNimi=$lahiesimiehenNimi, " +
            "lahiesimiesHyvaksynyt=$lahiesimiesHyvaksynyt, " +
            "lahiesimiehenKuittausaika=$lahiesimiehenKuittausaika, " +
            "muokkauspaiva=$muokkauspaiva, " +
            "korjausehdotus=$korjausehdotus, " +
            "erikoistuvaAllekirjoittanut=$erikoistuvaAllekirjoittanut)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
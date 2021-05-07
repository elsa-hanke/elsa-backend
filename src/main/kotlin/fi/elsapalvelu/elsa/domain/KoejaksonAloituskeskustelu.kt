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
        if (other !is KoejaksonAloituskeskustelu) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "Aloituskeskustelu(" +
            "id=$id, " +
            "erikoistuvaLaakari=$erikoistuvaLaakari, " +
            "erikoistuvanNimi=$erikoistuvanNimi, " +
            "erikoistuvanErikoisala=$erikoistuvanErikoisala, " +
            "erikoistuvanOpiskelijatunnus=$erikoistuvanOpiskelijatunnus, " +
            "erikoistuvanYliopisto=$erikoistuvanYliopisto, " +
            "erikoistuvanSahkoposti=$erikoistuvanSahkoposti, " +
            "koejaksonSuorituspaikka=$koejaksonSuorituspaikka, " +
            "koejaksonToinenSuorituspaikka=$koejaksonToinenSuorituspaikka, " +
            "koejaksonAlkamispaiva=$koejaksonAlkamispaiva, " +
            "koejaksonPaattymispaiva=$koejaksonPaattymispaiva, " +
            "suoritettuKokoaikatyossa=$suoritettuKokoaikatyossa, " +
            "tyotunnitViikossa=$tyotunnitViikossa, " +
            "lahikouluttaja=$lahikouluttaja, " +
            "lahikouluttajanNimi=$lahikouluttajanNimi, " +
            "lahikouluttajaHyvaksynyt=$lahikouluttajaHyvaksynyt, " +
            "lahikouluttajanKuittausaika=$lahikouluttajanKuittausaika, " +
            "lahiesimies=$lahiesimies, lahiesimiehenNimi=$lahiesimiehenNimi, " +
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

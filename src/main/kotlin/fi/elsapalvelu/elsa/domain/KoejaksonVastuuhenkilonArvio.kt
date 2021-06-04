package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "koejakson_vastuuhenkilon_arvio")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoejaksonVastuuhenkilonArvio(

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
    @ManyToOne(optional = false)
    var vastuuhenkilo: Kayttaja? = null,

    @NotNull
    @Column(name = "vastuuhenkilon_nimi", nullable = false)
    var vastuuhenkilonNimi: String? = null,

    @Column(name = "vastuuhenkilo_hyvaksynyt")
    var vastuuhenkiloHyvaksynyt: Boolean = false,

    @Column(name = "vastuuhenkilon_kuittausaika")
    var vastuuhenkilonKuittausaika: LocalDate? = null,

    @NotNull
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null,

    @Column(name = "erikoistuva_allekirjoittanut")
    var erikoistuvaAllekirjoittanut: Boolean = false

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
            "erikoistuvaLaakari=$erikoistuvaLaakari, " +
            "erikoistuvanNimi=$erikoistuvanNimi, " +
            "erikoistuvanErikoisala=$erikoistuvanErikoisala, " +
            "erikoistuvanOpiskelijatunnus=$erikoistuvanOpiskelijatunnus, " +
            "erikoistuvanYliopisto=$erikoistuvanYliopisto, " +
            "vastuuhenkilo=$vastuuhenkilo, " +
            "vastuuhenkilonNimi=$vastuuhenkilonNimi, " +
            "vastuuhenkiloHyvaksynyt=$vastuuhenkiloHyvaksynyt, " +
            "vastuuhenkilonKuittausaika=$vastuuhenkilonKuittausaika, " +
            "muokkauspaiva=$muokkauspaiva, " +
            "erikoistuvaAllekirjoittanut=$erikoistuvaAllekirjoittanut)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

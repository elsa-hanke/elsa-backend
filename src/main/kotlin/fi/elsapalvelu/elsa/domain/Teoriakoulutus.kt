package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "teoriakoulutus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Teoriakoulutus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "koulutuksen_nimi", nullable = false)
    var koulutuksenNimi: String? = null,

    @get: NotNull
    @Column(name = "koulutuksen_paikka", nullable = false)
    var koulutuksenPaikka: String? = null,

    @get: NotNull
    @Column(name = "alkamispaiva", nullable = false)
    var alkamispaiva: LocalDate? = null,

    @Column(name = "paattymispaiva")
    var paattymispaiva: LocalDate? = null,

    @get: Min(value = 0)
    @Column(name = "erikoistumiseen_hyvaksyttava_tuntimaara")
    var erikoistumiseenHyvaksyttavaTuntimaara: Int? = null,

    @OneToMany(
        mappedBy = "teoriakoulutus",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var todistukset: MutableSet<Asiakirja> = mutableSetOf(),

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Teoriakoulutus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Teoriakoulutus{" +
        "id=$id" +
        ", koulutuksenNimi='$koulutuksenNimi'" +
        ", koulutuksenPaikka='$koulutuksenPaikka'" +
        ", alkamispaiva='$alkamispaiva'" +
        ", paattymispaiva='$paattymispaiva'" +
        ", erikoistumiseenHyvaksyttavaTuntimaara=$erikoistumiseenHyvaksyttavaTuntimaara" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

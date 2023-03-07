package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "poissaolon_syy")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class PoissaolonSyy(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @get: NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vahennystyyppi", nullable = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var vahennystyyppi: PoissaolonSyyTyyppi? = null,

    @get: NotNull
    @Column(name = "vahennetaan_kerran", nullable = true)
    var vahennetaanKerran: Boolean = false,

    @get: NotNull
    @Column(name = "voimassaolon_alkamispaiva", nullable = false)
    var voimassaolonAlkamispaiva: LocalDate? = null,

    @Column(name = "voimassaolon_paattymispaiva")
    var voimassaolonPaattymispaiva: LocalDate? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PoissaolonSyy) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "PoissaolonSyy{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", vahennystyyppi='$vahennystyyppi'" +
        ", voimassaolonAlkamispaiva='$voimassaolonAlkamispaiva'" +
        ", voimassaolonPaattymispaiva='$voimassaolonPaattymispaiva'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

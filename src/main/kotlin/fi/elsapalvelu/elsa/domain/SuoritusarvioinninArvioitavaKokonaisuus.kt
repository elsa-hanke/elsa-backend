package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "suoritusarvioinnin_arvioitava_kokonaisuus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class SuoritusarvioinninArvioitavaKokonaisuus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @ManyToOne(optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var arvioitavaKokonaisuus: ArvioitavaKokonaisuus? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "itsearviointi_arviointiasteikon_taso")
    var itsearviointiArviointiasteikonTaso: Int? = null,

    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "arviointiasteikon_taso")
    var arviointiasteikonTaso: Int? = null,

    @NotNull
    @ManyToOne(optional = false)
    var suoritusarviointi: Suoritusarviointi? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuoritusarvioinninArvioitavaKokonaisuus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString(): String {
        return "SuoritusarvioinninArvioitavaKokonaisuus(id=$id, arvioitavaKokonaisuus=$arvioitavaKokonaisuus, itsearviointiArviointiasteikonTaso=$itsearviointiArviointiasteikonTaso, arviointiasteikonTaso=$arviointiasteikonTaso, suoritusarviointi=$suoritusarviointi)"
    }


    companion object {
        private const val serialVersionUID = 1L
    }
}

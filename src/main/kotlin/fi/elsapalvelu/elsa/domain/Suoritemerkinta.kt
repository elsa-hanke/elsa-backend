package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.*

@Entity
@Table(name = "suoritemerkinta")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Suoritemerkinta(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "suorituspaiva", nullable = false)
    var suorituspaiva: LocalDate? = null,

    @get: NotNull
    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "luottamuksen_taso", nullable = false)
    var luottamuksenTaso: Int? = null,

    @get: NotNull
    @get: Min(value = 1)
    @get: Max(value = 5)
    @Column(name = "vaativuustaso", nullable = false)
    var vaativuustaso: Int? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "lisatiedot")
    var lisatiedot: String? = null,

    @get: NotNull
    @Column(name = "lukittu", nullable = false)
    var lukittu: Boolean = false,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["suoritemerkinnat"], allowSetters = true)
    var oppimistavoite: Oppimistavoite? = null,

    @NotNull
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = ["suoritemerkinnat"], allowSetters = true)
    var tyoskentelyjakso: Tyoskentelyjakso? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Suoritemerkinta) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Suoritemerkinta{" +
        "id=$id" +
        ", suorituspaiva='$suorituspaiva'" +
        ", luottamuksenTaso=$luottamuksenTaso" +
        ", vaativuustaso=$vaativuustaso" +
        ", lisatiedot='$lisatiedot'" +
        ", lukittu='$lukittu'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

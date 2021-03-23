package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = "koulutussopimuksen_koulutuspaikka")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoulutussopimuksenKoulutuspaikka(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "yliopisto", nullable = false)
    var yliopisto: String? = null,

    @ManyToOne
    @JsonIgnoreProperties(value = ["koulutussopimuksenKoulutuspaikat"], allowSetters = true)
    var koulutussopimus: KoejaksonKoulutussopimus? = null

) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KoulutussopimuksenKoulutuspaikka

        if (id != other.id) return false
        if (nimi != other.nimi) return false
        if (yliopisto != other.yliopisto) return false
        if (koulutussopimus != other.koulutussopimus) return false

        return true
    }

    override fun toString(): String {
        return "KoulutussopimuksenKoulutuspaikka(id=$id, nimi=$nimi, yliopisto=$yliopisto, koulutussopimus=$koulutussopimus)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

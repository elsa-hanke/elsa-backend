package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "paivakirja_aihekategoria")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class PaivakirjaAihekategoria(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "kuvaus")
    var kuvaus: String? = null,

    @Column(name = "jarjestysnumero")
    var jarjestysnumero: Int? = null,

    @get: NotNull
    @Column(name = "teoriakoulutus", nullable = false)
    var teoriakoulutus: Boolean = false,

    @get: NotNull
    @Column(name = "muun_aiheen_nimi", nullable = false)
    var muunAiheenNimi: Boolean = false

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaivakirjaAihekategoria) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "PaivakirjaAihekategoria{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", jarjestysnumero=$jarjestysnumero" +
        ", teoriakoulutus='$teoriakoulutus'" +
        ", muunAiheenNimi='$muunAiheenNimi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

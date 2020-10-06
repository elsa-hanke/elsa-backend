package fi.oulu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.*

/**
 * A Osoite.
 */
@Entity
@Table(name = "osoite")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Osoite(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @get: NotNull
    @Column(name = "ensisijainen", nullable = false)
    var ensisijainen: Boolean? = null,

    @get: NotNull
    @Column(name = "osoiterivi_1", nullable = false)
    var osoiterivi1: String? = null,

    @Column(name = "osoiterivi_2")
    var osoiterivi2: String? = null,

    @Column(name = "osoiterivi_3")
    var osoiterivi3: String? = null,

    @get: NotNull
    @Column(name = "kunta", nullable = false)
    var kunta: String? = null,

    @get: NotNull
    @Column(name = "postinumero", nullable = false)
    var postinumero: Int? = null,

    @Column(name = "maakunta")
    var maakunta: String? = null,

    @get: NotNull
    @Column(name = "maa", nullable = false)
    var maa: String? = null,

    @ManyToOne @JsonIgnoreProperties(value = ["osoites"], allowSetters = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null

    // jhipster-needle-entity-add-field - JHipster will add fields here
) : Serializable {
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Osoite) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Osoite{" +
        "id=$id" +
        ", ensisijainen='$ensisijainen'" +
        ", osoiterivi1='$osoiterivi1'" +
        ", osoiterivi2='$osoiterivi2'" +
        ", osoiterivi3='$osoiterivi3'" +
        ", kunta='$kunta'" +
        ", postinumero=$postinumero" +
        ", maakunta='$maakunta'" +
        ", maa='$maa'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

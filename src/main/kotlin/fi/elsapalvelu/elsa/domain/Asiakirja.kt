package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.sun.istack.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "asiakirja")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Asiakirja(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(unique = true)
    var erikoistuvaLaakari: ErikoistuvaLaakari? = null,

    @ManyToOne(optional = true)
    @JsonIgnoreProperties(value = ["asiakirjat"], allowSetters = true)
    var tyoskentelyjakso: Tyoskentelyjakso? = null,

    @NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @NotNull
    @Column(name = "tyyppi", nullable = false)
    var tyyppi: String? = null,

    @NotNull
    @Column(name = "lisattypvm", nullable = false)
    var lisattypvm: LocalDateTime? = null,

    @NotNull
    @OneToOne(optional = false, cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    var asiakirjaData: AsiakirjaData? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asiakirja) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Asiakirja{" +
        "id=$id" +
        ", nimi='$nimi'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

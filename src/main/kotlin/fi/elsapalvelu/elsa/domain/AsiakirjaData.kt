package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sun.istack.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.sql.Blob
import javax.persistence.*

@Entity
@Table(name = "asiakirja_data")
@Cache(usage = CacheConcurrencyStrategy.NONE)
data class AsiakirjaData (
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @OneToOne(mappedBy = "asiakirjaData")
    @JsonIgnore
    var asiakirja: Asiakirja? = null,

    @NotNull
    @Lob
    @Column(name = "data", nullable = false)
    var data: Blob? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AsiakirjaData) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "AsiakirjaData{id=$id}"

    companion object {
        private const val serialVersionUID = 1L
    }

}

package fi.elsapalvelu.elsa.domain.arkistointi

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.io.Serializable

@Entity
@Table(name = "arkistointitehtava_asiakirja")
data class ArkistointitehtavaAsiakirja(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get:NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "arkistointitehtava_id", nullable = false)
    var arkistointitehtava: Arkistointitehtava? = null,

    @get:NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "asiakirja_id", nullable = false)
    var asiakirja: Asiakirja? = null,

    @get:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "asiakirjatyyppi", nullable = false, length = 50)
    var asiakirjatyyppi: RecordType? = null,

    @get:NotNull
    @get:Min(0)
    @Column(name = "jarjestys", nullable = false)
    var jarjestys: Int? = null,

    @get:NotNull
    @get:Size(max = 255)
    @Column(name = "tiedostonimi", nullable = false, length = 255)
    var tiedostonimi: String? = null,

    @get:NotNull
    @get:Size(max = 255)
    @Column(name = "sisaltotyyppi", nullable = false, length = 255)
    var sisaltotyyppi: String? = null,

    @get:NotNull
    @get:Pattern(regexp = "^[0-9a-fA-F]{64}$")
    @Column(name = "sha256", nullable = false, length = 64)
    var sha256: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArkistointitehtavaAsiakirja) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "ArkistointitehtavaAsiakirja{" +
        "id=$id" +
        ", asiakirjaId=${asiakirja?.id}" +
        ", asiakirjatyyppi=$asiakirjatyyppi" +
        ", jarjestys=$jarjestys" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

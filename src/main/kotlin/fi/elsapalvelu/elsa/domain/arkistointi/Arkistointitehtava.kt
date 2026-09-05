package fi.elsapalvelu.elsa.domain.arkistointi

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.io.Serializable
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "arkistointitehtava")
data class Arkistointitehtava(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "yliopisto", nullable = false, length = 50)
    var yliopisto: YliopistoEnum? = null,

    @get:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "asiatyyppi", nullable = false, length = 50)
    var asiatyyppi: CaseType? = null,

    @get:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tila", nullable = false, length = 30)
    var tila: ArkistointitehtavanTila = ArkistointitehtavanTila.ODOTTAA,

    @get:NotNull
    @Column(name = "metatiedot", nullable = false, columnDefinition = "text")
    var metatiedot: String? = null,

    @get:NotNull
    @get:Size(max = 100)
    @Column(name = "metatietoversio", nullable = false, length = 100)
    var metatietoversio: String? = null,

    @get:NotNull
    @get:Size(max = 255)
    @Column(name = "idempotenssiavain", nullable = false, unique = true, length = 255)
    var idempotenssiavain: String? = null,

    @get:Min(0)
    @Column(name = "yrityskerrat", nullable = false)
    var yrityskerrat: Int = 0,

    @get:NotNull
    @Column(name = "seuraava_kasittelyaika", nullable = false)
    var seuraavaKasittelyaika: Instant = Instant.now(),

    @Column(name = "kasittelyvaraus_paattyy")
    var kasittelyvarausPaattyy: Instant? = null,

    @get:Size(max = 255)
    @Column(name = "ulkoinen_toimitustunniste", length = 255)
    var ulkoinenToimitustunniste: String? = null,

    @get:Size(max = 100)
    @Column(name = "virhekoodi", length = 100)
    var virhekoodi: String? = null,

    @get:Size(max = 1000)
    @Column(name = "virhekuvaus", length = 1000)
    var virhekuvaus: String? = null,

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    var createdDate: Instant? = Instant.now(),

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    var lastModifiedDate: Instant? = Instant.now(),

    @Column(name = "onnistunut_toimitus_aika")
    var onnistunutToimitusAika: Instant? = null,

    @OneToMany(
        mappedBy = "arkistointitehtava",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @OrderBy("jarjestys ASC")
    var asiakirjat: MutableList<ArkistointitehtavaAsiakirja> = mutableListOf()

) : Serializable {

    fun lisaaAsiakirja(asiakirja: ArkistointitehtavaAsiakirja) {
        asiakirja.arkistointitehtava = this
        asiakirjat.add(asiakirja)
    }

    fun siirryTilaan(uusiTila: ArkistointitehtavanTila) {
        require(tila.salliiSiirtyman(uusiTila)) {
            "Arkistointitehtava ei voi siirtya tilasta $tila tilaan $uusiTila"
        }
        tila = uusiTila
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Arkistointitehtava) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Arkistointitehtava{" +
        "id=$id" +
        ", yliopisto=$yliopisto" +
        ", asiatyyppi=$asiatyyppi" +
        ", tila=$tila" +
        ", yrityskerrat=$yrityskerrat" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

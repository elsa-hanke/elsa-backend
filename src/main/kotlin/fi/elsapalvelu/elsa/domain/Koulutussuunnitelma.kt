package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "koulutussuunnitelma")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Koulutussuunnitelma(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "motivaatiokirje")
    var motivaatiokirje: String? = null,

    @get: NotNull
    @Column(name = "motivaatiokirje_yksityinen", nullable = false)
    var motivaatiokirjeYksityinen: Boolean = false,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "opiskelu_ja_tyohistoria")
    var opiskeluJaTyohistoria: String? = null,

    @get: NotNull
    @Column(name = "opiskelu_ja_tyohistoria_yksityinen", nullable = false)
    var opiskeluJaTyohistoriaYksityinen: Boolean = false,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "vahvuudet")
    var vahvuudet: String? = null,

    @get: NotNull
    @Column(name = "vahvuudet_yksityinen", nullable = false)
    var vahvuudetYksityinen: Boolean = false,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "tulevaisuuden_visiointi")
    var tulevaisuudenVisiointi: String? = null,

    @get: NotNull
    @Column(name = "tulevaisuuden_visiointi_yksityinen", nullable = false)
    var tulevaisuudenVisiointiYksityinen: Boolean = false,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "osaamisen_kartuttaminen")
    var osaamisenKartuttaminen: String? = null,

    @get: NotNull
    @Column(name = "osaamisen_kartuttaminen_yksityinen", nullable = false)
    var osaamisenKartuttaminenYksityinen: Boolean = false,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "elamankentta")
    var elamankentta: String? = null,

    @get: NotNull
    @Column(name = "elamankentta_yksityinen", nullable = false)
    var elamankenttaYksityinen: Boolean = false,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var opintooikeus: Opintooikeus? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(unique = true)
    var koulutussuunnitelmaAsiakirja: Asiakirja? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(unique = true)
    var motivaatiokirjeAsiakirja: Asiakirja? = null,

    @OneToMany(mappedBy = "koulutussuunnitelma")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    var koulutusjaksot: MutableSet<Koulutusjakso>? = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Koulutussuunnitelma) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Koulutussuunnitelma{" +
        "id=$id" +
        ", motivaatiokirje='$motivaatiokirje'" +
        ", motivaatiokirjeYksityinen='$motivaatiokirjeYksityinen'" +
        ", opiskeluJaTyohistoria='$opiskeluJaTyohistoria'" +
        ", opiskeluJaTyohistoriaYksityinen='$opiskeluJaTyohistoriaYksityinen'" +
        ", vahvuudet='$vahvuudet'" +
        ", vahvuudetYksityinen='$vahvuudetYksityinen'" +
        ", tulevaisuudenVisiointi='$tulevaisuudenVisiointi'" +
        ", tulevaisuudenVisiointiYksityinen='$tulevaisuudenVisiointiYksityinen'" +
        ", osaamisenKartuttaminen='$osaamisenKartuttaminen'" +
        ", osaamisenKartuttaminenYksityinen='$osaamisenKartuttaminenYksityinen'" +
        ", elamankentta='$elamankentta'" +
        ", elamankenttaYksityinen='$elamankenttaYksityinen'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

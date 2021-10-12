package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Type
import java.io.Serializable
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "kunta")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Kunta(

    @Id
    var id: String? = null,

    @get: NotNull
    @Column(name = "abbreviation", nullable = false)
    var abbreviation: String? = null,

    @Column(name = "short_name")
    var shortName: String? = null,

    @Column(name = "long_name")
    var longName: String? = null,

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "description")
    var description: String? = null,

    @Column(name = "kortnamn")
    var kortnamn: String? = null,

    @Column(name = "korvaava_koodi")
    var korvaavaKoodi: String? = null,

    @Column(name = "langt_namn")
    var langtNamn: String? = null,

    @Column(name = "maakunta")
    var maakunta: String? = null,

    @Column(name = "sairaanhoitopiiri")
    var sairaanhoitopiiri: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Kunta) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Kunta{" +
        "id=$id" +
        ", abbreviation='$abbreviation'" +
        ", shortName='$shortName'" +
        ", longName='$longName'" +
        ", description='$description'" +
        ", kortnamn='$kortnamn'" +
        ", korvaavaKoodi='$korvaavaKoodi'" +
        ", langtNamn='$langtNamn'" +
        ", maakunta='$maakunta'" +
        ", sairaanhoitopiiri='$sairaanhoitopiiri'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}

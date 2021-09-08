package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import javax.persistence.Lob
import javax.validation.constraints.Email
import javax.validation.constraints.Size

data class KayttajaDTO(
    var id: String? = null,

    @field:Size(max = 50)
    var etunimi: String? = null,

    @field:Size(max = 50)
    var sukunimi: String? = null,

    var nimike: String? = null,

    @field:Email
    @field:Size(min = 5, max = 254)
    var sahkopostiosoite: String? = null,

    @field:Size(max = 254)
    var puhelinnumero: String? = null,

    @Lob
    var profiilikuva: ByteArray? = null,

    var profiilikuvaContentType: String? = null,

    var yliopisto: YliopistoDTO? = null,

    var authorities: Set<String>? = null

) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KayttajaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "KayttajaDTO{" +
        "id=$id" +
        ", etunimi='" + etunimi + '\'' +
        ", sukunimi='" + sukunimi + '\'' +
        "}"
}

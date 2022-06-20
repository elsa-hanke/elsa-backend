package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import java.io.Serializable
import javax.persistence.Lob
import javax.validation.constraints.Email
import javax.validation.constraints.NotNull

data class KayttajaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null,

    var etunimi: String? = null,

    var sukunimi: String? = null,

    @get: Email
    var sahkoposti: String? = null,

    var puhelin: String? = null,

    var tila: KayttajatilinTila? = null,

    var eppn: String? = null,

    @Lob
    var avatar: ByteArray? = null,

    var userId: String? = null,

    var authorities: MutableSet<Authority>? = null,

    var nimike: String? = null,

    var yliopistotAndErikoisalat: MutableSet<KayttajaYliopistoErikoisalaDTO>? = mutableSetOf(),

    var yliopistot: MutableSet<YliopistoDTO>? = mutableSetOf()

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KayttajaDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "KayttajaDTO{" +
        "id=$id" +
        ", nimi='$nimi'" +
        "}"
}

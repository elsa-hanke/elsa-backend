package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class KayttajahallintaFormVastuuhenkiloDTO(

    var id: Long? = null,

    var etunimi: String? = null,

    var sukunimi: String? = null,

    var yliopistotAndErikoisalat: MutableSet<KayttajaYliopistoErikoisalaDTO>? = mutableSetOf(),

): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KayttajahallintaFormVastuuhenkiloDTO) return false
        return id != null && id == other.id
    }

    override fun hashCode() = 31
}

package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import java.io.Serializable
import java.time.LocalDate

data class ValmistumispyyntoListItemDTO(

    var id: Long? = null,

    var erikoistujanNimi: String? = null,

    var tila: ValmistumispyynnonTila? = null,

    var tapahtumanAjankohta: LocalDate? = null,

    var isAvoinForCurrentKayttaja: Boolean = false

): Serializable {
    override fun toString() = "ValmistumispyyntoListItemDTO"
}

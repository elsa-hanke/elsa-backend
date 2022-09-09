package fi.elsapalvelu.elsa.service.dto

import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import java.io.Serializable
import java.time.LocalDate

data class TerveyskeskuskoulutusjaksoSimpleDTO(

    var id: Long? = null,

    var tila: TerveyskeskuskoulutusjaksoTila? = null,

    var erikoistuvanNimi: String? = null,

    var pvm: LocalDate? = null

) : Serializable {
    override fun toString() = "TerveyskeskuskouluitusjaksoSimpleDTO"
}

package fi.elsapalvelu.elsa.service.dto.valmistuminen

import fi.elsapalvelu.elsa.service.dto.enumeration.TerveyskeskuskoulutusjaksoTila
import java.io.Serializable

data class TerveyskeskuskoulutusjaksoSimpleDTO(

    var id: Long? = null,

    var tila: TerveyskeskuskoulutusjaksoTila? = null,

    var erikoistuvanNimi: String? = null,

    var pvm: LocalDate? = null

) : Serializable {
    override fun toString() = "TerveyskeskuskouluitusjaksoSimpleDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

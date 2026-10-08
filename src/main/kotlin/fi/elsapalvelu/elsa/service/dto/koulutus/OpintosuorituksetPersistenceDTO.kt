package fi.elsapalvelu.elsa.service.dto.koulutus

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import java.io.Serializable

data class OpintosuorituksetPersistenceDTO(

    var yliopisto: YliopistoEnum,

    var items: List<OpintosuoritusDTO>? = null,

    // Only a complete source snapshot may remove missing children. Null child lists remain unchanged.
    var replaceOsakokonaisuudet: Boolean = false

) : Serializable {
    override fun toString() = "OpintosuorituksetPersistenceDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

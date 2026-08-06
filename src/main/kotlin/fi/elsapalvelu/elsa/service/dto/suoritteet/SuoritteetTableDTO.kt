package fi.elsapalvelu.elsa.service.dto.suoritteet

import java.io.Serializable
import java.util.*

import fi.elsapalvelu.elsa.service.dto.arviointi.ArviointiasteikkoDTO
class SuoritteetTableDTO(

    var suoritteenKategoriat: Set<SuoritteenKategoriaDTO> = setOf(),

    var aiemmatKategoriat: Set<SuoritteenKategoriaDTO> = setOf(),

    var suoritemerkinnat: Set<SuoritemerkintaDTO> = setOf(),

    var arviointiasteikko: ArviointiasteikkoDTO? = null

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun toString() = "SuoritteetTableDTO"
}

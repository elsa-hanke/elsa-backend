package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.util.*

class SuoritteetTableDTO(

    var suoritteenKategoriat: Set<SuoritteenKategoriaDTO> = setOf(),

    var aiemmatKategoriat: Set<SuoritteenKategoriaDTO> = setOf(),

    var suoritemerkinnat: Set<SuoritemerkintaDTO> = setOf(),

    var arviointiasteikko: ArviointiasteikkoDTO? = null

) : Serializable {
    override fun toString() = "SuoritteetTableDTO"
}

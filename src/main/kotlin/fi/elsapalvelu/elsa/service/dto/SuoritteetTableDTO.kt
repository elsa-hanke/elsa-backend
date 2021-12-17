package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.util.*

class SuoritteetTableDTO(

    var suoritteenKategoriat: SortedSet<SuoritteenKategoriaDTO> = sortedSetOf(),

    var suoritemerkinnat: Set<SuoritemerkintaDTO> = setOf(),

    var opintooppaat: Set<OpintoopasDTO> = setOf()

) : Serializable {
    override fun toString() = "SuoritteetTableDTO"
}

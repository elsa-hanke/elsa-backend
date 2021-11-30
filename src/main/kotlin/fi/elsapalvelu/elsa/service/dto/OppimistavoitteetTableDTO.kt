package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable
import java.util.*

class OppimistavoitteetTableDTO(

    var oppimistavoitteenKategoriat: SortedSet<OppimistavoitteenKategoriaDTO> = sortedSetOf(),

    var suoritemerkinnat: Set<SuoritemerkintaDTO> = setOf()

) : Serializable {
    override fun toString() = "OppimistavoitteetTableDTO"
}

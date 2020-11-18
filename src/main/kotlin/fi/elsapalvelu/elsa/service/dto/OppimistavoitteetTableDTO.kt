package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class OppimistavoitteetTableDTO(

    var oppimistavoitteenKategoriat: MutableSet<OppimistavoitteenKategoriaDTO> = mutableSetOf(),

    var suoritemerkinnat: MutableSet<SuoritemerkintaDTO> = mutableSetOf(),

) : Serializable {
    override fun hashCode() = 31
}

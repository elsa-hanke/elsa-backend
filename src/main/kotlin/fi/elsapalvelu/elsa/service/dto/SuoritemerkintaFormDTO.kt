package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class SuoritemerkintaFormDTO(

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

    var oppimistavoitteenKategoriat: MutableSet<OppimistavoitteenKategoriaDTO> = mutableSetOf()

) : Serializable {
    override fun hashCode() = 31
}

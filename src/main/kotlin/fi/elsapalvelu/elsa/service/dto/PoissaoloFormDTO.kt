package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

class PoissaoloFormDTO(

    var poissaoloSyyt: MutableSet<PoissaolonSyyDTO> = mutableSetOf(),

    var tyoskentelyjaksot: MutableSet<TyoskentelyjaksoDTO> = mutableSetOf(),

) : Serializable

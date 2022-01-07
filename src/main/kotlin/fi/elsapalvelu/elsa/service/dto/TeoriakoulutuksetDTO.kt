package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class TeoriakoulutuksetDTO(

    var teoriakoulutukset: MutableSet<TeoriakoulutusDTO> = mutableSetOf(),

    var erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: Double

) : Serializable {
    override fun toString() = "TeoriakoulutuksetDTO"
}

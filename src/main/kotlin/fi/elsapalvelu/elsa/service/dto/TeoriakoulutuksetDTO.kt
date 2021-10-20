package fi.elsapalvelu.elsa.service.dto

import java.io.Serializable

data class TeoriakoulutuksetDTO(

    var teoriakoulutukset: MutableSet<TeoriakoulutusDTO> = mutableSetOf(),

    var erikoisala: ErikoisalaDTO? = null

) : Serializable {
    override fun toString() = "TeoriakoulutuksetDTO"
}

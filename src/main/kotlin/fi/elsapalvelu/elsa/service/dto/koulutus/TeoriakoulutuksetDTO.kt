package fi.elsapalvelu.elsa.service.dto.koulutus

import java.io.Serializable

data class TeoriakoulutuksetDTO(

    var teoriakoulutukset: MutableSet<TeoriakoulutusDTO> = mutableSetOf(),

    var erikoisalanVaatimaTeoriakoulutustenVahimmaismaara: Double

) : Serializable {
    override fun toString() = "TeoriakoulutuksetDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

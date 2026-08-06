package fi.elsapalvelu.elsa.service.dto.seuranta

import java.io.Serializable

import fi.elsapalvelu.elsa.service.dto.koulutus.TeoriakoulutusDTO
class PaivakirjamerkintaFormDTO(

    var aihekategoriat: MutableSet<PaivakirjaAihekategoriaDTO> = mutableSetOf(),

    var teoriakoulutukset: MutableSet<TeoriakoulutusDTO> = mutableSetOf()

) : Serializable {
    override fun toString() = "PaivakirjamerkintaFormDTO"

    companion object {
        private const val serialVersionUID = 1L
    }
}

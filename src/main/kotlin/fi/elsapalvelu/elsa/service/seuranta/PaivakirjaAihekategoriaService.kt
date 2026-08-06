package fi.elsapalvelu.elsa.service.seuranta

import fi.elsapalvelu.elsa.service.dto.seuranta.PaivakirjaAihekategoriaDTO
import java.util.*

interface PaivakirjaAihekategoriaService {

    fun findAll(): List<PaivakirjaAihekategoriaDTO>

    fun findOne(id: Long): Optional<PaivakirjaAihekategoriaDTO>

}

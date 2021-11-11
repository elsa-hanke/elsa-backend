package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.PaivakirjaAihekategoriaDTO
import java.util.*

interface PaivakirjaAihekategoriaService {

    fun findAll(): List<PaivakirjaAihekategoriaDTO>

    fun findOne(id: Long): Optional<PaivakirjaAihekategoriaDTO>

}

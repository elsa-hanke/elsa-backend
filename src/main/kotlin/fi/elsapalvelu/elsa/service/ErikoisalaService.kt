package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

interface ErikoisalaService {

    fun save(erikoisalaDTO: ErikoisalaDTO): ErikoisalaDTO

    fun findAll(pageable: Pageable): Page<ErikoisalaDTO>

    fun findAll(): List<ErikoisalaDTO>

    fun findOne(id: Long): Optional<ErikoisalaDTO>

    fun delete(id: Long)
}

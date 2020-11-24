package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface YliopistoService {

    fun save(yliopistoDTO: YliopistoDTO): YliopistoDTO

    fun findAll(pageable: Pageable): Page<YliopistoDTO>

    fun findAllWithEagerRelationships(pageable: Pageable): Page<YliopistoDTO>

    fun findOne(id: Long): Optional<YliopistoDTO>

    fun delete(id: Long)
}

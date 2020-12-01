package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KuntaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface KuntaService {

    fun save(kuntaDTO: KuntaDTO): KuntaDTO

    fun findAll(pageable: Pageable): Page<KuntaDTO>

    fun findAll(): MutableList<KuntaDTO>

    fun findOne(id: String): KuntaDTO?

    fun delete(id: String)
}

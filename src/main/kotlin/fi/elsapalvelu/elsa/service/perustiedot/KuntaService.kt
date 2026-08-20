package fi.elsapalvelu.elsa.service.perustiedot

import fi.elsapalvelu.elsa.service.dto.perustiedot.KuntaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface KuntaService {

    fun save(kuntaDTO: KuntaDTO): KuntaDTO

    fun findAll(pageable: Pageable): Page<KuntaDTO>

    fun findAll(): List<KuntaDTO>

    fun findOne(id: String): KuntaDTO?

    fun delete(id: String)
}

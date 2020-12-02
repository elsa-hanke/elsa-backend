package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface KeskeytysaikaService {

    fun save(keskeytysaikaDTO: KeskeytysaikaDTO): KeskeytysaikaDTO

    fun findAll(pageable: Pageable): Page<KeskeytysaikaDTO>

    fun findOne(id: Long): KeskeytysaikaDTO?

    fun delete(id: Long)
}

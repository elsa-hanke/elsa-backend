package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.OppimistavoiteDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface OppimistavoiteService {

    fun save(oppimistavoiteDTO: OppimistavoiteDTO): OppimistavoiteDTO

    fun findAll(pageable: Pageable): Page<OppimistavoiteDTO>

    fun findOne(id: Long): Optional<OppimistavoiteDTO>

    fun delete(id: Long)
}

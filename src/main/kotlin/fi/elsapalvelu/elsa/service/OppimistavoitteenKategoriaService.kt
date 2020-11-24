package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteenKategoriaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface OppimistavoitteenKategoriaService {

    fun save(oppimistavoitteenKategoriaDTO: OppimistavoitteenKategoriaDTO): OppimistavoitteenKategoriaDTO

    fun findAll(): List<OppimistavoitteenKategoriaDTO>

    fun findAllByErikoisalaId(id: Long): List<OppimistavoitteenKategoriaDTO>

    fun findAll(pageable: Pageable): Page<OppimistavoitteenKategoriaDTO>

    fun findOne(id: Long): Optional<OppimistavoitteenKategoriaDTO>

    fun delete(id: Long)
}

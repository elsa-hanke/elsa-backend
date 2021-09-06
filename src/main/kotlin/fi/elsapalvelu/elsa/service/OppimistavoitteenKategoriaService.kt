package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.OppimistavoitteenKategoriaDTO
import java.util.*

interface OppimistavoitteenKategoriaService {

    fun save(oppimistavoitteenKategoriaDTO: OppimistavoitteenKategoriaDTO): OppimistavoitteenKategoriaDTO

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<OppimistavoitteenKategoriaDTO>

    fun findOne(id: Long): Optional<OppimistavoitteenKategoriaDTO>

    fun delete(id: Long)
}

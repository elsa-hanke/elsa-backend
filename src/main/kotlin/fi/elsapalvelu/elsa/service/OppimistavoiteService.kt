package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.OppimistavoiteDTO
import java.util.*

interface OppimistavoiteService {

    fun save(oppimistavoiteDTO: OppimistavoiteDTO): OppimistavoiteDTO

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<OppimistavoiteDTO>

    fun findOne(id: Long): Optional<OppimistavoiteDTO>

    fun delete(id: Long)
}

package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
import java.util.*

interface OsaalueenArviointiService {

    fun save(osaalueenArviointiDTO: OsaalueenArviointiDTO): OsaalueenArviointiDTO

    fun findAll(): List<OsaalueenArviointiDTO>

    fun findOne(id: Long): Optional<OsaalueenArviointiDTO>

    fun delete(id: Long)
}

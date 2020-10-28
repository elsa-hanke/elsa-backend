package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
import java.util.Optional

interface OsaalueenArviointiService {

    fun save(osaalueenArviointiDTO: OsaalueenArviointiDTO): OsaalueenArviointiDTO

    fun findAll(): MutableList<OsaalueenArviointiDTO>

    fun findOne(id: Long): Optional<OsaalueenArviointiDTO>

    fun delete(id: Long)
}

package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.ArvioitavaOsaalueDTO
import java.util.Optional

interface ArvioitavaOsaalueService {

    fun save(arvioitavaOsaalueDTO: ArvioitavaOsaalueDTO): ArvioitavaOsaalueDTO

    fun findAll(): MutableList<ArvioitavaOsaalueDTO>

    fun findOne(id: Long): Optional<ArvioitavaOsaalueDTO>

    fun delete(id: Long)
}

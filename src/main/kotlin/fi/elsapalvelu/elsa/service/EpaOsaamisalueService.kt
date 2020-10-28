package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
import java.util.Optional

interface EpaOsaamisalueService {

    fun save(epaOsaamisalueDTO: EpaOsaamisalueDTO): EpaOsaamisalueDTO

    fun findAll(): MutableList<EpaOsaamisalueDTO>

    fun findOne(id: Long): Optional<EpaOsaamisalueDTO>

    fun delete(id: Long)
}

package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.SuoriteDTO
import java.util.*

interface SuoriteService {

    fun save(suoriteDTO: SuoriteDTO): SuoriteDTO

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<SuoriteDTO>

    fun findOne(id: Long): Optional<SuoriteDTO>

    fun delete(id: Long)
}

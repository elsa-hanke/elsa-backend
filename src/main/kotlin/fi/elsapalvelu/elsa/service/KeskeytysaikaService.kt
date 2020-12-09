package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO

interface KeskeytysaikaService {

    fun save(keskeytysaikaDTO: KeskeytysaikaDTO, userId: String): KeskeytysaikaDTO?

    fun findOne(id: Long, userId: String): KeskeytysaikaDTO?

    fun delete(id: Long, userId: String)
}

package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO

interface KeskeytysaikaService {

    fun save(keskeytysaikaDTO: KeskeytysaikaDTO, opintooikeusId: Long): KeskeytysaikaDTO?

    fun findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId: Long): List<KeskeytysaikaDTO>

    fun findOne(id: Long, opintooikeusId: Long): KeskeytysaikaDTO?

    fun delete(id: Long, opintooikeusId: Long)
}

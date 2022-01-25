package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.PoissaolonSyyDTO

interface PoissaolonSyyService {

    fun save(poissaolonSyyDTO: PoissaolonSyyDTO): PoissaolonSyyDTO

    fun findAllByOpintooikeusId(opintooikeusId: Long): List<PoissaolonSyyDTO>

    fun findOne(id: Long): PoissaolonSyyDTO?

    fun delete(id: Long)
}

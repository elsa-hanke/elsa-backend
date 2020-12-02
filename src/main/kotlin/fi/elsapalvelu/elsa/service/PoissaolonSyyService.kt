package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.PoissaolonSyyDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PoissaolonSyyService {

    fun save(poissaolonSyyDTO: PoissaolonSyyDTO): PoissaolonSyyDTO

    fun findAll(pageable: Pageable): Page<PoissaolonSyyDTO>

    fun findAll(): MutableList<PoissaolonSyyDTO>

    fun findOne(id: Long): PoissaolonSyyDTO?

    fun delete(id: Long)
}

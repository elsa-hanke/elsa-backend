package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import java.util.Optional

interface KayttajaService {

    fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO

    fun findAll(): MutableList<KayttajaDTO>

    fun findOne(id: Long): Optional<KayttajaDTO>

    fun delete(id: Long)
}

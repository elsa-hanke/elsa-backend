package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import java.util.Optional

interface KayttajaService {

    fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO

    fun save(kayttajaDTO: KayttajaDTO, userDTO: UserDTO): KayttajaDTO

    fun findAll(): MutableList<KayttajaDTO>

    fun findOne(id: Long): Optional<KayttajaDTO>

    fun delete(id: Long)
}

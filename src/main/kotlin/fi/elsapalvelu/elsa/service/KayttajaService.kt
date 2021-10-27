package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import java.util.*

interface KayttajaService {

    fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO

    fun save(kayttajaDTO: KayttajaDTO, userDTO: UserDTO): KayttajaDTO

    fun findAll(): List<KayttajaDTO>

    fun findOne(id: Long): Optional<KayttajaDTO>

    fun findByUserId(id: String): Optional<KayttajaDTO>

    fun findKouluttajat(): List<KayttajaDTO>

    fun findVastuuhenkilot(): List<KayttajaDTO>

    fun findTeknisetPaakayttajat(): List<KayttajaDTO>

    fun findKouluttajatAndVastuuhenkilot(userId: String): List<KayttajaDTO>

    fun delete(id: Long)
}

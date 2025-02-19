package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import fi.elsapalvelu.elsa.service.dto.UserDTO
import java.util.*

interface ArviointityokaluService {

    fun save(
        arviointityokaluDTO: ArviointityokaluDTO,
        user: UserDTO
    ): ArviointityokaluDTO

    fun findAll(): List<ArviointityokaluDTO>

    fun findAllByKayttajaUserId(userId: String): MutableList<ArviointityokaluDTO>

    fun findOne(id: Long): Optional<ArviointityokaluDTO>

    fun delete(id: Long)

    fun update(arviointityokaluDTO: ArviointityokaluDTO): ArviointityokaluDTO?

}

package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import java.util.*

interface ErikoistuvaLaakariService {

    fun save(erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO

    fun findAll(): List<ErikoistuvaLaakariDTO>

    fun findOne(id: Long): Optional<ErikoistuvaLaakariDTO>

    fun delete(id: Long)

    fun findOneByKayttajaUserId(id: String): ErikoistuvaLaakariDTO?
}

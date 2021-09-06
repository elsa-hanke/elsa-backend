package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import java.util.*

interface ErikoisalaService {

    fun save(erikoisalaDTO: ErikoisalaDTO): ErikoisalaDTO

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<ErikoisalaDTO>

    fun findOne(id: Long): Optional<ErikoisalaDTO>

    fun delete(id: Long)
}

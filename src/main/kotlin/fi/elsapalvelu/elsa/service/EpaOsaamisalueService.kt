package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
import java.util.*

interface EpaOsaamisalueService {

    fun save(epaOsaamisalueDTO: EpaOsaamisalueDTO): EpaOsaamisalueDTO

    fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<EpaOsaamisalueDTO>

    fun findOne(id: Long): Optional<EpaOsaamisalueDTO>

    fun delete(id: Long)
}

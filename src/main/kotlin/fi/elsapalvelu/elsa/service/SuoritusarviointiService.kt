package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import java.util.*

interface SuoritusarviointiService {
    fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO

    fun save(suoritusarviointiDTO: SuoritusarviointiDTO, userId: String): SuoritusarviointiDTO

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): List<SuoritusarviointiDTO>

    fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<SuoritusarviointiDTO>

    fun findOneByIdAndArvioinninAntajauserId(
        id: Long,
        userId: String
    ): Optional<SuoritusarviointiDTO>

    fun delete(id: Long, userId: String)
}

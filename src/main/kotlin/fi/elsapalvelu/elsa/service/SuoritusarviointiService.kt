package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface SuoritusarviointiService {
    fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO

    fun save(suoritusarviointiDTO: SuoritusarviointiDTO, userId: String): SuoritusarviointiDTO

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): MutableList<SuoritusarviointiDTO>

    fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<SuoritusarviointiDTO>

    fun findOneByIdAndArvioinninAntajauserLogin(
        id: Long,
        userLogin: String
    ): Optional<SuoritusarviointiDTO>

    fun delete(id: Long, userId: String)
}

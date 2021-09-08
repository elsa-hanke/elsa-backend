package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import java.util.*

interface SuoritusarviointiService {
    fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO

    fun save(suoritusarviointiDTO: SuoritusarviointiDTO, kayttajaId: String): SuoritusarviointiDTO

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaId(
        kayttajaId: String
    ): List<SuoritusarviointiDTO>

    fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<SuoritusarviointiDTO>

    fun findOneByIdAndArvioinninAntajaId(
        id: Long,
        kayttajaId: String
    ): Optional<SuoritusarviointiDTO>

    fun delete(id: Long, kayttajaId: String)
}

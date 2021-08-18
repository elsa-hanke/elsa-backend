package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinninKommenttiDTO
import java.util.*

interface SuoritusarvioinninKommenttiService {

    fun save(
        suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO,
        userId: String
    ): SuoritusarvioinninKommenttiDTO

    fun findAll(): List<SuoritusarvioinninKommenttiDTO>

    fun findOne(id: Long): Optional<SuoritusarvioinninKommenttiDTO>

    fun delete(id: Long)
}

package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.SuoritusarvioinninKommenttiDTO
import java.util.Optional

interface SuoritusarvioinninKommenttiService {

    fun save(suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO): SuoritusarvioinninKommenttiDTO

    fun save(
        suoritusarvioinninKommenttiDTO: SuoritusarvioinninKommenttiDTO,
        userId: String
    ): SuoritusarvioinninKommenttiDTO

    fun findAll(): MutableList<SuoritusarvioinninKommenttiDTO>

    fun findOne(id: Long): Optional<SuoritusarvioinninKommenttiDTO>

    fun delete(id: Long)
}

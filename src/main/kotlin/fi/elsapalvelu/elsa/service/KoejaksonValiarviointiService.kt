package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonValiarviointiDTO
import java.util.*

interface KoejaksonValiarviointiService {

    fun create(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        kayttajaId: String
    ): KoejaksonValiarviointiDTO

    fun update(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        kayttajaId: String
    ): KoejaksonValiarviointiDTO

    fun findOne(id: Long): Optional<KoejaksonValiarviointiDTO>

    fun findByErikoistuvaLaakariKayttajaId(
        kayttajaId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun findAllByKouluttajaId(
        kayttajaId: String
    ): Map<KayttajaDTO, KoejaksonValiarviointiDTO>

    fun delete(id: Long)
}

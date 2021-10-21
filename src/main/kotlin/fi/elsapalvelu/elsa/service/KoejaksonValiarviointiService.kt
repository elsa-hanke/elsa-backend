package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonValiarviointiDTO
import java.util.*

interface KoejaksonValiarviointiService {

    fun create(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        userId: String
    ): KoejaksonValiarviointiDTO

    fun update(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        userId: String
    ): KoejaksonValiarviointiDTO

    fun findOne(id: Long): Optional<KoejaksonValiarviointiDTO>

    fun findByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun findAllByKouluttajaUserId(
        userId: String
    ): Map<KayttajaDTO, KoejaksonValiarviointiDTO>

    fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun delete(id: Long)
}

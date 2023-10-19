package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksonValiarviointiDTO
import java.util.*

interface KoejaksonValiarviointiService {

    fun create(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        opintooikeusId: Long
    ): KoejaksonValiarviointiDTO?

    fun update(
        koejaksonValiarviointiDTO: KoejaksonValiarviointiDTO,
        userId: String
    ): KoejaksonValiarviointiDTO

    fun findOne(id: Long): Optional<KoejaksonValiarviointiDTO>

    fun findByOpintooikeusId(
        opintooikeusId: Long
    ): Optional<KoejaksonValiarviointiDTO>

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonValiarviointiDTO>

    fun delete(id: Long, userId: String)

}

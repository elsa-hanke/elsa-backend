package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import java.util.*

interface KoejaksonVastuuhenkilonArvioService {

    fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        opintooikeusId: Long
    ): KoejaksonVastuuhenkilonArvioDTO?

    fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVastuuhenkilonArvioDTO

    fun findOne(id: Long): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun findByOpintooikeusId(
        opintooikeusId: Long
    ): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun existsByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Boolean

    fun findOneByIdAndVirkailijaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun delete(id: Long)

    fun tarkistaAllekirjoitus(koejaksonVastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio)
}

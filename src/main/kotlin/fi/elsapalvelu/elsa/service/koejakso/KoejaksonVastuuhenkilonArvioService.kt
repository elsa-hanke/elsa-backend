package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import java.util.*

interface KoejaksonVastuuhenkilonArvioService {

    fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        opintooikeusId: Long,
        asiakirjat: Set<AsiakirjaDTO>?
    ): KoejaksonVastuuhenkilonArvioDTO?

    fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String,
        asiakirjat: Set<AsiakirjaDTO>? = null,
        deletedAsiakirjaIds: Set<Int>? = null
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
}

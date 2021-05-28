package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.*
import java.util.*

interface KoejaksonVastuuhenkilonArvioService {

    fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVastuuhenkilonArvioDTO

    fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        userId: String
    ): KoejaksonVastuuhenkilonArvioDTO

    fun findOne(id: Long): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun findByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun findAllByVastuuhenkiloUserId(
        userId: String
    ): Map<KayttajaDTO, KoejaksonVastuuhenkilonArvioDTO>

    fun delete(id: Long)
}

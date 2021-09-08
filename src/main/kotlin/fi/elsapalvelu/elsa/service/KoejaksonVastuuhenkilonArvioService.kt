package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonVastuuhenkilonArvioDTO
import java.util.*

interface KoejaksonVastuuhenkilonArvioService {

    fun create(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        kayttajaId: String
    ): KoejaksonVastuuhenkilonArvioDTO

    fun update(
        koejaksonVastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO,
        kayttajaId: String
    ): KoejaksonVastuuhenkilonArvioDTO

    fun findOne(id: Long): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun findByErikoistuvaLaakariKayttajaId(
        kayttajaId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun findOneByIdAndVastuuhenkiloId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonVastuuhenkilonArvioDTO>

    fun findAllByVastuuhenkiloId(
        kayttajaId: String
    ): Map<KayttajaDTO, KoejaksonVastuuhenkilonArvioDTO>

    fun delete(id: Long)
}

package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import java.util.*

interface KoejaksonKoulutussopimusService {

    fun create(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        kayttajaId: String
    ): KoejaksonKoulutussopimusDTO

    fun update(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        kayttajaId: String
    ): KoejaksonKoulutussopimusDTO

    fun findOne(id: Long): Optional<KoejaksonKoulutussopimusDTO>

    fun findByErikoistuvaLaakariKayttajaId(
        kayttajaId: String
    ): Optional<KoejaksonKoulutussopimusDTO>

    fun findOneByIdAndKouluttajaKayttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKoulutussopimusDTO>

    fun findAllByKouluttajaKayttajaId(
        kayttajaId: String
    ): Map<KayttajaDTO, KoejaksonKoulutussopimusDTO>

    fun findAllByVastuuhenkiloKayttajaId(
        kayttajaId: String
    ): Map<KayttajaDTO, KoejaksonKoulutussopimusDTO>

    fun findOneByIdAndVastuuhenkiloKayttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKoulutussopimusDTO>

    fun delete(id: Long)
}

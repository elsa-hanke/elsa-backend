package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonKehittamistoimenpiteetDTO
import java.util.*

interface KoejaksonKehittamistoimenpiteetService {

    fun create(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        kayttajaId: String
    ): KoejaksonKehittamistoimenpiteetDTO

    fun update(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        kayttajaId: String
    ): KoejaksonKehittamistoimenpiteetDTO

    fun findOne(id: Long): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findByErikoistuvaLaakariKayttajaId(
        kayttajaId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findAllByKouluttajaId(
        kayttajaId: String
    ): Map<KayttajaDTO, KoejaksonKehittamistoimenpiteetDTO>

    fun delete(id: Long)
}

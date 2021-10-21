package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonKehittamistoimenpiteetDTO
import java.util.*

interface KoejaksonKehittamistoimenpiteetService {

    fun create(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        userId: String
    ): KoejaksonKehittamistoimenpiteetDTO

    fun update(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        userId: String
    ): KoejaksonKehittamistoimenpiteetDTO

    fun findOne(id: Long): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findAllByKouluttajaUserId(
        userId: String
    ): Map<KayttajaDTO, KoejaksonKehittamistoimenpiteetDTO>

    fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun delete(id: Long)
}

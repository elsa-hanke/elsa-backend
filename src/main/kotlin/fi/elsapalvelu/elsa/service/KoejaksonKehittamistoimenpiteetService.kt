package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksonKehittamistoimenpiteetDTO
import java.util.*

interface KoejaksonKehittamistoimenpiteetService {

    fun create(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        opintooikeusId: Long
    ): KoejaksonKehittamistoimenpiteetDTO?

    fun update(
        koejaksonKehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO,
        userId: String
    ): KoejaksonKehittamistoimenpiteetDTO

    fun findOne(id: Long): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findByOpintooikeusId(
        opintooikeusId: Long
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun findOneByIdHyvaksyttyAndBelongsToVastuuhenkilo(
        id: Long,
        vastuuhenkiloUserId: String
    ): Optional<KoejaksonKehittamistoimenpiteetDTO>

    fun delete(id: Long, userId: String)

}

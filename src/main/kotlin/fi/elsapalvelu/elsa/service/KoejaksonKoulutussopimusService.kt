package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import java.util.*

interface KoejaksonKoulutussopimusService {

    fun create(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        opintooikeusId: Long
    ): KoejaksonKoulutussopimusDTO?

    fun update(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        userId: String
    ): KoejaksonKoulutussopimusDTO

    fun findOne(id: Long): Optional<KoejaksonKoulutussopimusDTO>

    fun findByOpintooikeusId(
        opintooikeusId: Long
    ): Optional<KoejaksonKoulutussopimusDTO>

    fun findOneByIdAndKouluttajaKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimusDTO>

    fun findOneByIdAndVastuuhenkiloKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonKoulutussopimusDTO>

    fun delete(id: Long)
}

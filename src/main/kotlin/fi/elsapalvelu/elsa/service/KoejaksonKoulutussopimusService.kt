package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import java.util.*

interface KoejaksonKoulutussopimusService {

    fun save(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        koejaksoId: Long,
        userId: String
    ): KoejaksonKoulutussopimusDTO

    fun findOne(id: Long): Optional<KoejaksonKoulutussopimusDTO>

    fun delete(id: Long)
}

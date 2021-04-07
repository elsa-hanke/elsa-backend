package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksonAloituskeskusteluDTO
import java.util.*

interface KoejaksonAloituskeskusteluService {

    fun create(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        userId: String
    ): KoejaksonAloituskeskusteluDTO

    fun update(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        userId: String
    ): KoejaksonAloituskeskusteluDTO

    fun findOne(id: Long): Optional<KoejaksonAloituskeskusteluDTO>

    fun findByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): Optional<KoejaksonAloituskeskusteluDTO>

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonAloituskeskusteluDTO>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonAloituskeskusteluDTO>

    fun delete(id: Long)
}

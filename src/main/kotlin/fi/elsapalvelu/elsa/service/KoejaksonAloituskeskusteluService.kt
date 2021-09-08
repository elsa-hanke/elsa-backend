package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonAloituskeskusteluDTO
import java.util.*

interface KoejaksonAloituskeskusteluService {

    fun create(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        kayttajaId: String
    ): KoejaksonAloituskeskusteluDTO

    fun update(
        koejaksonAloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO,
        kayttajaId: String
    ): KoejaksonAloituskeskusteluDTO

    fun findOne(id: Long): Optional<KoejaksonAloituskeskusteluDTO>

    fun findByErikoistuvaLaakariKayttajaId(
        kayttajaId: String
    ): Optional<KoejaksonAloituskeskusteluDTO>

    fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonAloituskeskusteluDTO>

    fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonAloituskeskusteluDTO>

    fun findAllByKouluttajaId(
        kayttajaId: String
    ): Map<KayttajaDTO, KoejaksonAloituskeskusteluDTO>

    fun delete(id: Long)
}

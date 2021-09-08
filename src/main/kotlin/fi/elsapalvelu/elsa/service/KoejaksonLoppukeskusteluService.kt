package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KoejaksonLoppukeskusteluDTO
import java.util.*

interface KoejaksonLoppukeskusteluService {

    fun create(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        kayttajaId: String
    ): KoejaksonLoppukeskusteluDTO

    fun update(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        kayttajaId: String
    ): KoejaksonLoppukeskusteluDTO

    fun findOne(id: Long): Optional<KoejaksonLoppukeskusteluDTO>

    fun findByErikoistuvaLaakariKayttajaId(
        kayttajaId: String
    ): Optional<KoejaksonLoppukeskusteluDTO>

    fun findOneByIdAndLahikouluttajaId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonLoppukeskusteluDTO>

    fun findOneByIdAndLahiesimiesId(
        id: Long,
        kayttajaId: String
    ): Optional<KoejaksonLoppukeskusteluDTO>

    fun findAllByKouluttajaId(
        kayttajaId: String
    ): Map<KayttajaDTO, KoejaksonLoppukeskusteluDTO>

    fun delete(id: Long)
}

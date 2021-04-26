package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.*
import java.util.*

interface KoejaksonLoppukeskusteluService {

    fun create(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        userId: String
    ): KoejaksonLoppukeskusteluDTO

    fun update(
        koejaksonLoppukeskusteluDTO: KoejaksonLoppukeskusteluDTO,
        userId: String
    ): KoejaksonLoppukeskusteluDTO

    fun findOne(id: Long): Optional<KoejaksonLoppukeskusteluDTO>

    fun findByErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): Optional<KoejaksonLoppukeskusteluDTO>

    fun findOneByIdAndLahikouluttajaUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonLoppukeskusteluDTO>

    fun findOneByIdAndLahiesimiesUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonLoppukeskusteluDTO>

    fun findAllByKouluttajaUserId(
        userId: String
    ): Map<KayttajaDTO, KoejaksonLoppukeskusteluDTO>

    fun delete(id: Long)
}

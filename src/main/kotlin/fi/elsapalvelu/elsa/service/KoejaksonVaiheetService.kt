package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.criteria.KoejaksoCriteria
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface KoejaksonVaiheetService {
    fun findAllByKouluttajaKayttajaUserId(userId: String): List<KoejaksonVaiheDTO>

    fun findAllByVastuuhenkiloKayttajaUserId(userId: String): List<KoejaksonVaiheDTO>

    fun findAllByVirkailijaKayttajaUserId(
        userId: String,
        criteria: KoejaksoCriteria,
        pageable: Pageable
    ): Page<KoejaksonVaiheDTO>?
}

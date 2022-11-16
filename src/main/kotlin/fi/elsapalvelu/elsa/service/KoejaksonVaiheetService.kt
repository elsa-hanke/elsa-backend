package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.criteria.AvoinAndNimiCriteria
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.KoejaksonVaiheDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface KoejaksonVaiheetService {

    fun findAllByKouluttajaKayttajaUserId(
        userId: String,
        vainAvoimet: Boolean = false
    ): List<KoejaksonVaiheDTO>

    fun findAllByVastuuhenkiloKayttajaUserId(
        userId: String,
        avoinAndNimiCriteria: AvoinAndNimiCriteria = AvoinAndNimiCriteria(false),
        pageable: Pageable
    ): Page<KoejaksonVaiheDTO>

    fun findAllByVirkailijaKayttajaUserId(
        userId: String,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<KoejaksonVaiheDTO>?

    fun findAllAvoinByVirkailijaKayttajaUserId(
        userId: String
    ): List<KoejaksonVaiheDTO>?
}

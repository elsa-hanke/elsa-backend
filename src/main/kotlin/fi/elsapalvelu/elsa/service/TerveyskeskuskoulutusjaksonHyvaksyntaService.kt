package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoSimpleDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TerveyskeskuskoulutusjaksonHyvaksyntaService {

    fun findByIdAndYliopistoId(
        id: Long,
        isVastuuhenkilo: Boolean,
        yliopistoIds: List<Long>
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun findByOpintooikeusId(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun findByVirkailijaUserId(
        userId: String,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<TerveyskeskuskoulutusjaksoSimpleDTO>?

    fun findByVastuuhenkiloUserId(
        userId: String,
        criteria: NimiErikoisalaAndAvoinCriteria,
        pageable: Pageable
    ): Page<TerveyskeskuskoulutusjaksoSimpleDTO>?

    fun findByOpintooikeusIdOrCreateNew(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun getTerveyskoulutusjaksoSuoritettu(opintooikeus: Opintooikeus): Boolean

    fun create(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun update(
        userId: String,
        isVirkailija: Boolean,
        id: Long,
        korjausehdotus: String?,
        lisatiedotVirkailijalta: String?
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun existsByOpintooikeusId(opintooikeusId: Long): Boolean
}

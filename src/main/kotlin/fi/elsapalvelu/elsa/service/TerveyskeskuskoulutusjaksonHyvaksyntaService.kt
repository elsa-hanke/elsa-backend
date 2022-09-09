package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.service.criteria.TerveyskeskuskoulutusjaksoCriteria
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksoSimpleDTO
import fi.elsapalvelu.elsa.service.dto.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TerveyskeskuskoulutusjaksonHyvaksyntaService {

    fun findById(id: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun findByOpintooikeusId(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun findByVirkailijaUserId(
        userId: String,
        criteria: TerveyskeskuskoulutusjaksoCriteria,
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

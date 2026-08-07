package fi.elsapalvelu.elsa.service.valmistuminen

import fi.elsapalvelu.elsa.service.criteria.NimiErikoisalaAndAvoinCriteria
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksoSimpleDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.multipart.MultipartFile

interface TerveyskeskuskoulutusjaksonHyvaksyntaService {

    fun findByIdAndYliopistoIdVirkailija(
        id: Long,
        yliopistoIds: List<Long>
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun findByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
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

    fun getTerveyskoulutusjaksoSuoritettu(opintooikeusId: Long): Boolean

    fun getTerveyskoulutusjaksoSuoritettuYek(opintooikeusId: Long): Boolean

    fun create(opintooikeusId: Long): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun update(
        userId: String,
        isVirkailija: Boolean,
        id: Long,
        korjausehdotus: String?,
        lisatiedotVirkailijalta: String?,
        laillistamispaiva: LocalDate? = null,
        laillistamistodistus: MultipartFile? = null
    ): TerveyskeskuskoulutusjaksonHyvaksyntaDTO?

    fun existsByOpintooikeusId(opintooikeusId: Long): Boolean
}

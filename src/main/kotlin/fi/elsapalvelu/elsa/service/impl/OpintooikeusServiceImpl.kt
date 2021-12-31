package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.OpintooikeusService
import fi.elsapalvelu.elsa.service.dto.OpintooikeusDTO
import fi.elsapalvelu.elsa.service.mapper.OpintooikeusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OpintooikeusServiceImpl(
    private val opintooikeusRepository: OpintooikeusRepository,
    private val opintooikeusMapper: OpintooikeusMapper
) : OpintooikeusService {
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<OpintooikeusDTO> {
        return opintooikeusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId).map(opintooikeusMapper::toDto)
    }
}

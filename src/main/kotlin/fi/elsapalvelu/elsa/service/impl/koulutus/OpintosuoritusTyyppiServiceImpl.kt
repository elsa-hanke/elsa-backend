package fi.elsapalvelu.elsa.service.impl.koulutus

import fi.elsapalvelu.elsa.repository.koulutus.OpintosuoritusTyyppiRepository
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusTyyppiService
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusTyyppiDTO
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusTyyppiMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OpintosuoritusTyyppiServiceImpl(
    private val opintosuoritusTyyppiRepository: OpintosuoritusTyyppiRepository,
    private val opintosuoritusTyyppiMapper: OpintosuoritusTyyppiMapper
) : OpintosuoritusTyyppiService {
    override fun findAll(): List<OpintosuoritusTyyppiDTO>? {
        return opintosuoritusTyyppiRepository.findAll().map(opintosuoritusTyyppiMapper::toDto)
    }
}

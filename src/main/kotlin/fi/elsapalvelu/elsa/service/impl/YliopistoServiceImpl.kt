package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.YliopistoService
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import fi.elsapalvelu.elsa.service.mapper.YliopistoMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Yliopisto].
 */
@Service
@Transactional
class YliopistoServiceImpl(
    private val yliopistoRepository: YliopistoRepository,
    private val yliopistoMapper: YliopistoMapper
) : YliopistoService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(yliopistoDTO: YliopistoDTO): YliopistoDTO {
        log.debug("Request to save Yliopisto : $yliopistoDTO")

        var yliopisto = yliopistoMapper.toEntity(yliopistoDTO)
        yliopisto = yliopistoRepository.save(yliopisto)
        return yliopistoMapper.toDto(yliopisto)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<YliopistoDTO> {
        log.debug("Request to get all Yliopistos")
        return yliopistoRepository.findAllWithEagerRelationships()
            .mapTo(mutableListOf(), yliopistoMapper::toDto)
    }

    override fun findAllWithEagerRelationships(pageable: Pageable) =
        yliopistoRepository.findAllWithEagerRelationships(pageable).map(yliopistoMapper::toDto)

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<YliopistoDTO> {
        log.debug("Request to get Yliopisto : $id")
        return yliopistoRepository.findOneWithEagerRelationships(id)
            .map(yliopistoMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Yliopisto : $id")

        yliopistoRepository.deleteById(id)
    }
}

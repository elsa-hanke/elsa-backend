package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.YliopistoService
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import fi.elsapalvelu.elsa.service.mapper.YliopistoMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class YliopistoServiceImpl(
    private val yliopistoRepository: YliopistoRepository,
    private val yliopistoMapper: YliopistoMapper
) : YliopistoService {

    override fun save(yliopistoDTO: YliopistoDTO): YliopistoDTO {
        var yliopisto = yliopistoMapper.toEntity(yliopistoDTO)
        yliopisto = yliopistoRepository.save(yliopisto)
        return yliopistoMapper.toDto(yliopisto)
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<YliopistoDTO> {
        return yliopistoRepository.findAll(pageable)
            .map(yliopistoMapper::toDto)
    }

    override fun findAllWithEagerRelationships(pageable: Pageable) =
        yliopistoRepository.findAllWithEagerRelationships(pageable).map(yliopistoMapper::toDto)

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<YliopistoDTO> {
        return yliopistoRepository.findOneWithEagerRelationships(id)
            .map(yliopistoMapper::toDto)
    }

    override fun delete(id: Long) {
        yliopistoRepository.deleteById(id)
    }
}

package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.YliopistoService
import fi.elsapalvelu.elsa.service.dto.HakaYliopistoDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import fi.elsapalvelu.elsa.service.mapper.HakaYliopistoMapper
import fi.elsapalvelu.elsa.service.mapper.YliopistoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class YliopistoServiceImpl(
    private val yliopistoRepository: YliopistoRepository,
    private val yliopistoMapper: YliopistoMapper,
    private val hakaYliopistoMapper: HakaYliopistoMapper
) : YliopistoService {

    override fun save(yliopistoDTO: YliopistoDTO): YliopistoDTO {
        var yliopisto = yliopistoMapper.toEntity(yliopistoDTO)
        yliopisto = yliopistoRepository.save(yliopisto)
        return yliopistoMapper.toDto(yliopisto)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<YliopistoDTO> {
        return yliopistoRepository.findAll()
            .map(yliopistoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<YliopistoDTO> {
        return yliopistoRepository.findOneWithEagerRelationships(id)
            .map(yliopistoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllHaka(): List<HakaYliopistoDTO> {
        return yliopistoRepository.findAllHaka()
            .map(hakaYliopistoMapper::toDto)
    }

    override fun delete(id: Long) {
        yliopistoRepository.deleteById(id)
    }
}

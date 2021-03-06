package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.service.ErikoisalaService
import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class ErikoisalaServiceImpl(
    private val erikoisalaRepository: ErikoisalaRepository,
    private val erikoisalaMapper: ErikoisalaMapper
) : ErikoisalaService {

    override fun save(erikoisalaDTO: ErikoisalaDTO): ErikoisalaDTO {
        var erikoisala = erikoisalaMapper.toEntity(erikoisalaDTO)
        erikoisala = erikoisalaRepository.save(erikoisala)
        return erikoisalaMapper.toDto(erikoisala)
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<ErikoisalaDTO> {
        return erikoisalaRepository.findAll(pageable)
            .map(erikoisalaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<ErikoisalaDTO> {
        return erikoisalaRepository.findAll()
            .mapTo(mutableListOf(), erikoisalaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ErikoisalaDTO> {
        return erikoisalaRepository.findById(id)
            .map(erikoisalaMapper::toDto)
    }

    override fun delete(id: Long) {
        erikoisalaRepository.deleteById(id)
    }
}

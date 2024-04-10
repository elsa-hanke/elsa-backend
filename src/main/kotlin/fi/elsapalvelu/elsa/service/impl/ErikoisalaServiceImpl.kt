package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.service.ErikoisalaService
import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.ErikoisalaWithTehtavatyypitDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaWithTehtavatyypitMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class ErikoisalaServiceImpl(
    private val erikoisalaRepository: ErikoisalaRepository,
    private val erikoisalaMapper: ErikoisalaMapper,
    private val erikoisalaWithTehtavatyypitMapper: ErikoisalaWithTehtavatyypitMapper
) : ErikoisalaService {

    override fun save(erikoisalaDTO: ErikoisalaDTO): ErikoisalaDTO {
        var erikoisala = erikoisalaMapper.toEntity(erikoisalaDTO)
        erikoisala = erikoisalaRepository.save(erikoisala)
        return erikoisalaMapper.toDto(erikoisala)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ErikoisalaDTO> {
        return erikoisalaRepository.findAll()
            .map(erikoisalaMapper::toDto)
    }

    override fun findAllByLiittynytElsaan(): List<ErikoisalaDTO> {
        return erikoisalaRepository.findAllByLiittynytElsaanTrue()
            .map(erikoisalaMapper::toDto)
    }

    override fun findAllWithTehtavatyypit(): List<ErikoisalaWithTehtavatyypitDTO> {
        return erikoisalaRepository.findAll()
            .map(erikoisalaWithTehtavatyypitMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ErikoisalaDTO> {
        return erikoisalaRepository.findById(id)
            .map(erikoisalaMapper::toDto)
    }

    override fun delete(id: Long) {
        erikoisalaRepository.deleteById(id)
    }

    override fun findAllByIdIs(erikoisalaId: Long): List<ErikoisalaDTO> {
        return erikoisalaRepository.findAllByIdIs(erikoisalaId)
            .map(erikoisalaMapper::toDto)
    }

}

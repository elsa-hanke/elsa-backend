package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.Erikoisala
import fi.oulu.elsa.repository.ErikoisalaRepository
import fi.oulu.elsa.service.ErikoisalaService
import fi.oulu.elsa.service.dto.ErikoisalaDTO
import fi.oulu.elsa.service.mapper.ErikoisalaMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Erikoisala].
 */
@Service
@Transactional
class ErikoisalaServiceImpl(
    private val erikoisalaRepository: ErikoisalaRepository,
    private val erikoisalaMapper: ErikoisalaMapper
) : ErikoisalaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(erikoisalaDTO: ErikoisalaDTO): ErikoisalaDTO {
        log.debug("Request to save Erikoisala : $erikoisalaDTO")

        var erikoisala = erikoisalaMapper.toEntity(erikoisalaDTO)
        erikoisala = erikoisalaRepository.save(erikoisala)
        return erikoisalaMapper.toDto(erikoisala)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<ErikoisalaDTO> {
        log.debug("Request to get all Erikoisalas")
        return erikoisalaRepository.findAll()
            .mapTo(mutableListOf(), erikoisalaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ErikoisalaDTO> {
        log.debug("Request to get Erikoisala : $id")
        return erikoisalaRepository.findById(id)
            .map(erikoisalaMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Erikoisala : $id")

        erikoisalaRepository.deleteById(id)
    }
}

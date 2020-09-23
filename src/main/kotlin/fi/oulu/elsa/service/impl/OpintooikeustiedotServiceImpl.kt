package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.Opintooikeustiedot
import fi.oulu.elsa.repository.OpintooikeustiedotRepository
import fi.oulu.elsa.service.OpintooikeustiedotService
import fi.oulu.elsa.service.dto.OpintooikeustiedotDTO
import fi.oulu.elsa.service.mapper.OpintooikeustiedotMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Opintooikeustiedot].
 */
@Service
@Transactional
class OpintooikeustiedotServiceImpl(
    private val opintooikeustiedotRepository: OpintooikeustiedotRepository,
    private val opintooikeustiedotMapper: OpintooikeustiedotMapper
) : OpintooikeustiedotService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(opintooikeustiedotDTO: OpintooikeustiedotDTO): OpintooikeustiedotDTO {
        log.debug("Request to save Opintooikeustiedot : $opintooikeustiedotDTO")

        var opintooikeustiedot = opintooikeustiedotMapper.toEntity(opintooikeustiedotDTO)
        opintooikeustiedot = opintooikeustiedotRepository.save(opintooikeustiedot)
        return opintooikeustiedotMapper.toDto(opintooikeustiedot)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<OpintooikeustiedotDTO> {
        log.debug("Request to get all Opintooikeustiedots")
        return opintooikeustiedotRepository.findAll()
            .mapTo(mutableListOf(), opintooikeustiedotMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<OpintooikeustiedotDTO> {
        log.debug("Request to get Opintooikeustiedot : $id")
        return opintooikeustiedotRepository.findById(id)
            .map(opintooikeustiedotMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Opintooikeustiedot : $id")

        opintooikeustiedotRepository.deleteById(id)
    }
}

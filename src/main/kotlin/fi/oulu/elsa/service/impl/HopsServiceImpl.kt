package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.Hops
import fi.oulu.elsa.repository.HopsRepository
import fi.oulu.elsa.service.HopsService
import fi.oulu.elsa.service.dto.HopsDTO
import fi.oulu.elsa.service.mapper.HopsMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Hops].
 */
@Service
@Transactional
class HopsServiceImpl(
    private val hopsRepository: HopsRepository,
    private val hopsMapper: HopsMapper
) : HopsService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(hopsDTO: HopsDTO): HopsDTO {
        log.debug("Request to save Hops : $hopsDTO")

        var hops = hopsMapper.toEntity(hopsDTO)
        hops = hopsRepository.save(hops)
        return hopsMapper.toDto(hops)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<HopsDTO> {
        log.debug("Request to get all Hops")
        return hopsRepository.findAll()
            .mapTo(mutableListOf(), hopsMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<HopsDTO> {
        log.debug("Request to get Hops : $id")
        return hopsRepository.findById(id)
            .map(hopsMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Hops : $id")

        hopsRepository.deleteById(id)
    }
}

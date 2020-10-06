package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.ArvioitavaOsaalue
import fi.oulu.elsa.repository.ArvioitavaOsaalueRepository
import fi.oulu.elsa.service.ArvioitavaOsaalueService
import fi.oulu.elsa.service.dto.ArvioitavaOsaalueDTO
import fi.oulu.elsa.service.mapper.ArvioitavaOsaalueMapper
import java.util.Optional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service Implementation for managing [ArvioitavaOsaalue].
 */
@Service
@Transactional
class ArvioitavaOsaalueServiceImpl(
    private val arvioitavaOsaalueRepository: ArvioitavaOsaalueRepository,
    private val arvioitavaOsaalueMapper: ArvioitavaOsaalueMapper
) : ArvioitavaOsaalueService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(arvioitavaOsaalueDTO: ArvioitavaOsaalueDTO): ArvioitavaOsaalueDTO {
        log.debug("Request to save ArvioitavaOsaalue : $arvioitavaOsaalueDTO")

        var arvioitavaOsaalue = arvioitavaOsaalueMapper.toEntity(arvioitavaOsaalueDTO)
        arvioitavaOsaalue = arvioitavaOsaalueRepository.save(arvioitavaOsaalue)
        return arvioitavaOsaalueMapper.toDto(arvioitavaOsaalue)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<ArvioitavaOsaalueDTO> {
        log.debug("Request to get all ArvioitavaOsaalues")
        return arvioitavaOsaalueRepository.findAll()
            .mapTo(mutableListOf(), arvioitavaOsaalueMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArvioitavaOsaalueDTO> {
        log.debug("Request to get ArvioitavaOsaalue : $id")
        return arvioitavaOsaalueRepository.findById(id)
            .map(arvioitavaOsaalueMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete ArvioitavaOsaalue : $id")

        arvioitavaOsaalueRepository.deleteById(id)
    }
}

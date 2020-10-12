package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Arviointiosaalue
import fi.elsapalvelu.elsa.repository.ArviointiosaalueRepository
import fi.elsapalvelu.elsa.service.ArviointiosaalueService
import fi.elsapalvelu.elsa.service.dto.ArviointiosaalueDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointiosaalueMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Arviointiosaalue].
 */
@Service
@Transactional
class ArviointiosaalueServiceImpl(
    private val arviointiosaalueRepository: ArviointiosaalueRepository,
    private val arviointiosaalueMapper: ArviointiosaalueMapper
) : ArviointiosaalueService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(arviointiosaalueDTO: ArviointiosaalueDTO): ArviointiosaalueDTO {
        log.debug("Request to save Arviointiosaalue : $arviointiosaalueDTO")

        var arviointiosaalue = arviointiosaalueMapper.toEntity(arviointiosaalueDTO)
        arviointiosaalue = arviointiosaalueRepository.save(arviointiosaalue)
        return arviointiosaalueMapper.toDto(arviointiosaalue)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<ArviointiosaalueDTO> {
        log.debug("Request to get all Arviointiosaalues")
        return arviointiosaalueRepository.findAll()
            .mapTo(mutableListOf(), arviointiosaalueMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArviointiosaalueDTO> {
        log.debug("Request to get Arviointiosaalue : $id")
        return arviointiosaalueRepository.findById(id)
            .map(arviointiosaalueMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Arviointiosaalue : $id")

        arviointiosaalueRepository.deleteById(id)
    }
}

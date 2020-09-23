package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.Pikaviesti
import fi.oulu.elsa.repository.PikaviestiRepository
import fi.oulu.elsa.service.PikaviestiService
import fi.oulu.elsa.service.dto.PikaviestiDTO
import fi.oulu.elsa.service.mapper.PikaviestiMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Pikaviesti].
 */
@Service
@Transactional
class PikaviestiServiceImpl(
    private val pikaviestiRepository: PikaviestiRepository,
    private val pikaviestiMapper: PikaviestiMapper
) : PikaviestiService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(pikaviestiDTO: PikaviestiDTO): PikaviestiDTO {
        log.debug("Request to save Pikaviesti : $pikaviestiDTO")

        var pikaviesti = pikaviestiMapper.toEntity(pikaviestiDTO)
        pikaviesti = pikaviestiRepository.save(pikaviesti)
        return pikaviestiMapper.toDto(pikaviesti)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<PikaviestiDTO> {
        log.debug("Request to get all Pikaviestis")
        return pikaviestiRepository.findAll()
            .mapTo(mutableListOf(), pikaviestiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<PikaviestiDTO> {
        log.debug("Request to get Pikaviesti : $id")
        return pikaviestiRepository.findById(id)
            .map(pikaviestiMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Pikaviesti : $id")

        pikaviestiRepository.deleteById(id)
    }
}

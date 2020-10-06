package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.Suoritusarviointi
import fi.oulu.elsa.repository.SuoritusarviointiRepository
import fi.oulu.elsa.service.SuoritusarviointiService
import fi.oulu.elsa.service.dto.SuoritusarviointiDTO
import fi.oulu.elsa.service.mapper.SuoritusarviointiMapper
import java.util.Optional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service Implementation for managing [Suoritusarviointi].
 */
@Service
@Transactional
class SuoritusarviointiServiceImpl(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper
) : SuoritusarviointiService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(suoritusarviointiDTO: SuoritusarviointiDTO): SuoritusarviointiDTO {
        log.debug("Request to save Suoritusarviointi : $suoritusarviointiDTO")

        var suoritusarviointi = suoritusarviointiMapper.toEntity(suoritusarviointiDTO)
        suoritusarviointi = suoritusarviointiRepository.save(suoritusarviointi)
        return suoritusarviointiMapper.toDto(suoritusarviointi)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<SuoritusarviointiDTO> {
        log.debug("Request to get all Suoritusarviointis")
        return suoritusarviointiRepository.findAll()
            .mapTo(mutableListOf(), suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<SuoritusarviointiDTO> {
        log.debug("Request to get Suoritusarviointi : $id")
        return suoritusarviointiRepository.findById(id)
            .map(suoritusarviointiMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Suoritusarviointi : $id")

        suoritusarviointiRepository.deleteById(id)
    }
}

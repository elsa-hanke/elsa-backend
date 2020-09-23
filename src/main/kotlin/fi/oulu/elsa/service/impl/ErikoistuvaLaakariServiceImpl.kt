package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.ErikoistuvaLaakari
import fi.oulu.elsa.repository.ErikoistuvaLaakariRepository
import fi.oulu.elsa.service.ErikoistuvaLaakariService
import fi.oulu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.oulu.elsa.service.mapper.ErikoistuvaLaakariMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [ErikoistuvaLaakari].
 */
@Service
@Transactional
class ErikoistuvaLaakariServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val erikoistuvaLaakariMapper: ErikoistuvaLaakariMapper
) : ErikoistuvaLaakariService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO {
        log.debug("Request to save ErikoistuvaLaakari : $erikoistuvaLaakariDTO")

        var erikoistuvaLaakari = erikoistuvaLaakariMapper.toEntity(erikoistuvaLaakariDTO)
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
        return erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<ErikoistuvaLaakariDTO> {
        log.debug("Request to get all ErikoistuvaLaakaris")
        return erikoistuvaLaakariRepository.findAll()
            .mapTo(mutableListOf(), erikoistuvaLaakariMapper::toDto)
    }

    /**
     *  Get all the erikoistuvaLaakaris where Hops is `null`.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    override fun findAllWhereHopsIsNull(): MutableList<ErikoistuvaLaakariDTO> {
        log.debug("Request to get all erikoistuvaLaakaris where Hops is null")
        return erikoistuvaLaakariRepository.findAll()
            .asSequence()
            .filter { it.hops == null }
            .mapTo(mutableListOf()) { erikoistuvaLaakariMapper.toDto(it) }
    }

    /**
     *  Get all the erikoistuvaLaakaris where Koejakso is `null`.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    override fun findAllWhereKoejaksoIsNull(): MutableList<ErikoistuvaLaakariDTO> {
        log.debug("Request to get all erikoistuvaLaakaris where Koejakso is null")
        return erikoistuvaLaakariRepository.findAll()
            .asSequence()
            .filter { it.koejakso == null }
            .mapTo(mutableListOf()) { erikoistuvaLaakariMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ErikoistuvaLaakariDTO> {
        log.debug("Request to get ErikoistuvaLaakari : $id")
        return erikoistuvaLaakariRepository.findById(id)
            .map(erikoistuvaLaakariMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete ErikoistuvaLaakari : $id")

        erikoistuvaLaakariRepository.deleteById(id)
    }
}

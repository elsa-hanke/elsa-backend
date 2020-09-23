package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.PikaviestiKeskustelu
import fi.oulu.elsa.repository.PikaviestiKeskusteluRepository
import fi.oulu.elsa.service.PikaviestiKeskusteluService
import fi.oulu.elsa.service.dto.PikaviestiKeskusteluDTO
import fi.oulu.elsa.service.mapper.PikaviestiKeskusteluMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [PikaviestiKeskustelu].
 */
@Service
@Transactional
class PikaviestiKeskusteluServiceImpl(
    private val pikaviestiKeskusteluRepository: PikaviestiKeskusteluRepository,
    private val pikaviestiKeskusteluMapper: PikaviestiKeskusteluMapper
) : PikaviestiKeskusteluService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(pikaviestiKeskusteluDTO: PikaviestiKeskusteluDTO): PikaviestiKeskusteluDTO {
        log.debug("Request to save PikaviestiKeskustelu : $pikaviestiKeskusteluDTO")

        var pikaviestiKeskustelu = pikaviestiKeskusteluMapper.toEntity(pikaviestiKeskusteluDTO)
        pikaviestiKeskustelu = pikaviestiKeskusteluRepository.save(pikaviestiKeskustelu)
        return pikaviestiKeskusteluMapper.toDto(pikaviestiKeskustelu)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<PikaviestiKeskusteluDTO> {
        log.debug("Request to get all PikaviestiKeskustelus")
        return pikaviestiKeskusteluRepository.findAllWithEagerRelationships()
            .mapTo(mutableListOf(), pikaviestiKeskusteluMapper::toDto)
    }

    override fun findAllWithEagerRelationships(pageable: Pageable) =
        pikaviestiKeskusteluRepository.findAllWithEagerRelationships(pageable).map(pikaviestiKeskusteluMapper::toDto)

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<PikaviestiKeskusteluDTO> {
        log.debug("Request to get PikaviestiKeskustelu : $id")
        return pikaviestiKeskusteluRepository.findOneWithEagerRelationships(id)
            .map(pikaviestiKeskusteluMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete PikaviestiKeskustelu : $id")

        pikaviestiKeskusteluRepository.deleteById(id)
    }
}

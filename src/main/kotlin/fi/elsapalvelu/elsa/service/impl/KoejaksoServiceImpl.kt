package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Koejakso
import fi.elsapalvelu.elsa.repository.KoejaksoRepository
import fi.elsapalvelu.elsa.service.KoejaksoService
import fi.elsapalvelu.elsa.service.dto.KoejaksoDTO
import fi.elsapalvelu.elsa.service.mapper.KoejaksoMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Koejakso].
 */
@Service
@Transactional
class KoejaksoServiceImpl(
    private val koejaksoRepository: KoejaksoRepository,
    private val koejaksoMapper: KoejaksoMapper
) : KoejaksoService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(koejaksoDTO: KoejaksoDTO): KoejaksoDTO {
        log.debug("Request to save Koejakso : $koejaksoDTO")

        var koejakso = koejaksoMapper.toEntity(koejaksoDTO)
        koejakso = koejaksoRepository.save(koejakso)
        return koejaksoMapper.toDto(koejakso)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<KoejaksoDTO> {
        log.debug("Request to get all Koejaksos")
        return koejaksoRepository.findAll()
            .mapTo(mutableListOf(), koejaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KoejaksoDTO> {
        log.debug("Request to get Koejakso : $id")
        return koejaksoRepository.findById(id)
            .map(koejaksoMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Koejakso : $id")

        koejaksoRepository.deleteById(id)
    }
}

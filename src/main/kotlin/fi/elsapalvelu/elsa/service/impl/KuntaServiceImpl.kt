package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KuntaRepository
import fi.elsapalvelu.elsa.service.KuntaService
import fi.elsapalvelu.elsa.service.dto.KuntaDTO
import fi.elsapalvelu.elsa.service.mapper.KuntaMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KuntaServiceImpl(
    private val kuntaRepository: KuntaRepository,
    private val kuntaMapper: KuntaMapper
) : KuntaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(kuntaDTO: KuntaDTO): KuntaDTO {
        var kunta = kuntaMapper.toEntity(kuntaDTO)
        kunta = kuntaRepository.save(kunta)
        return kuntaMapper.toDto(kunta)
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<KuntaDTO> {
        return kuntaRepository.findAll(pageable)
            .map(kuntaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<KuntaDTO> {
        return kuntaRepository.findAll()
            .map(kuntaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: String): KuntaDTO? {
        kuntaRepository.findByIdOrNull(id)?.let {
            return kuntaMapper.toDto(it)
        }

        return null
    }

    override fun delete(id: String) {
        kuntaRepository.deleteById(id)
    }
}

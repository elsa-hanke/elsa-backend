package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.service.KeskeytysaikaService
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.mapper.KeskeytysaikaMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class KeskeytysaikaServiceImpl(
    private val keskeytysaikaRepository: KeskeytysaikaRepository,
    private val keskeytysaikaMapper: KeskeytysaikaMapper
) : KeskeytysaikaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(keskeytysaikaDTO: KeskeytysaikaDTO): KeskeytysaikaDTO {
        log.debug("Request to save Keskeytysaika : $keskeytysaikaDTO")

        var keskeytysaika = keskeytysaikaMapper.toEntity(keskeytysaikaDTO)
        keskeytysaika = keskeytysaikaRepository.save(keskeytysaika)
        return keskeytysaikaMapper.toDto(keskeytysaika)
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<KeskeytysaikaDTO> {
        log.debug("Request to get all Keskeytysaika")
        return keskeytysaikaRepository.findAll(pageable)
            .map(keskeytysaikaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): KeskeytysaikaDTO? {
        log.debug("Request to get Keskeytysaika : $id")
        keskeytysaikaRepository.findByIdOrNull(id)?.let {
            return keskeytysaikaMapper.toDto(it)
        }
        return null
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Keskeytysaika : $id")

        keskeytysaikaRepository.deleteById(id)
    }
}

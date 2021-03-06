package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.EpaOsaamisalueRepository
import fi.elsapalvelu.elsa.service.EpaOsaamisalueService
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
import fi.elsapalvelu.elsa.service.mapper.EpaOsaamisalueMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class EpaOsaamisalueServiceImpl(
    private val epaOsaamisalueRepository: EpaOsaamisalueRepository,
    private val epaOsaamisalueMapper: EpaOsaamisalueMapper
) : EpaOsaamisalueService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(epaOsaamisalueDTO: EpaOsaamisalueDTO): EpaOsaamisalueDTO {
        log.debug("Request to save EpaOsaamisalue : $epaOsaamisalueDTO")

        var epaOsaamisalue = epaOsaamisalueMapper.toEntity(epaOsaamisalueDTO)
        epaOsaamisalue = epaOsaamisalueRepository.save(epaOsaamisalue)
        return epaOsaamisalueMapper.toDto(epaOsaamisalue)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<EpaOsaamisalueDTO> {
        log.debug("Request to get all EpaOsaamisalueet")

        return epaOsaamisalueRepository.findAll()
            .mapTo(mutableListOf(), epaOsaamisalueMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<EpaOsaamisalueDTO> {
        log.debug("Request to get EpaOsaamisalue : $id")

        return epaOsaamisalueRepository.findById(id)
            .map(epaOsaamisalueMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete EpaOsaamisalue : $id")

        epaOsaamisalueRepository.deleteById(id)
    }
}

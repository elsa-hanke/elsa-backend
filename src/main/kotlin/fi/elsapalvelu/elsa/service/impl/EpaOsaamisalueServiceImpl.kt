package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.EpaOsaamisalueRepository
import fi.elsapalvelu.elsa.service.EpaOsaamisalueService
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
import fi.elsapalvelu.elsa.service.mapper.EpaOsaamisalueMapper
import java.util.Optional
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EpaOsaamisalueServiceImpl(
    private val epaOsaamisalueRepository: EpaOsaamisalueRepository,
    private val epaOsaamisalueMapper: EpaOsaamisalueMapper
) : EpaOsaamisalueService {

    override fun save(epaOsaamisalueDTO: EpaOsaamisalueDTO): EpaOsaamisalueDTO {
        var epaOsaamisalue = epaOsaamisalueMapper.toEntity(epaOsaamisalueDTO)
        epaOsaamisalue = epaOsaamisalueRepository.save(epaOsaamisalue)
        return epaOsaamisalueMapper.toDto(epaOsaamisalue)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<EpaOsaamisalueDTO> {
        return epaOsaamisalueRepository.findAll()
            .mapTo(mutableListOf(), epaOsaamisalueMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<EpaOsaamisalueDTO> {
        return epaOsaamisalueRepository.findById(id)
            .map(epaOsaamisalueMapper::toDto)
    }

    override fun delete(id: Long) {
        epaOsaamisalueRepository.deleteById(id)
    }
}

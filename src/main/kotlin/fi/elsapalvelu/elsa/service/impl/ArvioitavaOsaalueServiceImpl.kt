package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArvioitavaOsaalueRepository
import fi.elsapalvelu.elsa.service.ArvioitavaOsaalueService
import fi.elsapalvelu.elsa.service.dto.ArvioitavaOsaalueDTO
import fi.elsapalvelu.elsa.service.mapper.ArvioitavaOsaalueMapper
import java.util.Optional
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ArvioitavaOsaalueServiceImpl(
    private val arvioitavaOsaalueRepository: ArvioitavaOsaalueRepository,
    private val arvioitavaOsaalueMapper: ArvioitavaOsaalueMapper
) : ArvioitavaOsaalueService {

    override fun save(arvioitavaOsaalueDTO: ArvioitavaOsaalueDTO): ArvioitavaOsaalueDTO {
        var arvioitavaOsaalue = arvioitavaOsaalueMapper.toEntity(arvioitavaOsaalueDTO)
        arvioitavaOsaalue = arvioitavaOsaalueRepository.save(arvioitavaOsaalue)
        return arvioitavaOsaalueMapper.toDto(arvioitavaOsaalue)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<ArvioitavaOsaalueDTO> {
        return arvioitavaOsaalueRepository.findAll()
            .mapTo(mutableListOf(), arvioitavaOsaalueMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArvioitavaOsaalueDTO> {
        return arvioitavaOsaalueRepository.findById(id)
            .map(arvioitavaOsaalueMapper::toDto)
    }

    override fun delete(id: Long) {
        arvioitavaOsaalueRepository.deleteById(id)
    }
}

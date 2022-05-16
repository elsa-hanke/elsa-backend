package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.VastuuhenkilonTehtavatyyppiRepository
import fi.elsapalvelu.elsa.service.VastuuhenkilonTehtavatyyppiService
import fi.elsapalvelu.elsa.service.dto.VastuuhenkilonTehtavatyyppiDTO
import fi.elsapalvelu.elsa.service.mapper.VastuuhenkilonTehtavatyyppiMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VastuuhenkilonTehtavatyyppiServiceImpl(
    private val vastuuhenkilonTehtavatyyppiRepository: VastuuhenkilonTehtavatyyppiRepository,
    private val vastuuhenkilonTehtavatyyppiMapper: VastuuhenkilonTehtavatyyppiMapper
): VastuuhenkilonTehtavatyyppiService {
    override fun findAll(): List<VastuuhenkilonTehtavatyyppiDTO> {
        return vastuuhenkilonTehtavatyyppiRepository.findAll().map(vastuuhenkilonTehtavatyyppiMapper::toDto)
    }

    override fun findOne(id: Long): VastuuhenkilonTehtavatyyppiDTO? {
        return vastuuhenkilonTehtavatyyppiRepository.findByIdOrNull(id)?.let {
            return vastuuhenkilonTehtavatyyppiMapper.toDto(it)
        }
    }
}

package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.AsetusRepository
import fi.elsapalvelu.elsa.service.AsetusService
import fi.elsapalvelu.elsa.service.dto.AsetusDTO
import fi.elsapalvelu.elsa.service.mapper.AsetusMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AsetusServiceImpl(
    private val asetusRepository: AsetusRepository,
    private val asetusMapper: AsetusMapper
) : AsetusService {

    override fun findAll(): List<AsetusDTO> {
        return asetusRepository.findAll()
            .map(asetusMapper::toDto)
    }

    override fun findOne(id: Long): AsetusDTO? {
        return asetusRepository.findByIdOrNull(id)?.let {
            return asetusMapper.toDto(it)
        }
    }
}

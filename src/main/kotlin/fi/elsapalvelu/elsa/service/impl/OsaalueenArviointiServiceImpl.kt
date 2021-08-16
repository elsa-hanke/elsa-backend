package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.OsaalueenArviointiRepository
import fi.elsapalvelu.elsa.service.OsaalueenArviointiService
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
import fi.elsapalvelu.elsa.service.mapper.OsaalueenArviointiMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class OsaalueenArviointiServiceImpl(
    private val osaalueenArviointiRepository: OsaalueenArviointiRepository,
    private val osaalueenArviointiMapper: OsaalueenArviointiMapper
) : OsaalueenArviointiService {

    override fun save(osaalueenArviointiDTO: OsaalueenArviointiDTO): OsaalueenArviointiDTO {
        var osaalueenArviointi = osaalueenArviointiMapper.toEntity(osaalueenArviointiDTO)
        osaalueenArviointi = osaalueenArviointiRepository.save(osaalueenArviointi)
        return osaalueenArviointiMapper.toDto(osaalueenArviointi)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<OsaalueenArviointiDTO> {
        return osaalueenArviointiRepository.findAll()
            .map(osaalueenArviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<OsaalueenArviointiDTO> {
        return osaalueenArviointiRepository.findById(id)
            .map(osaalueenArviointiMapper::toDto)
    }

    override fun delete(id: Long) {
        osaalueenArviointiRepository.deleteById(id)
    }
}

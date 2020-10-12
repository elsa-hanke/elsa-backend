package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.OsaalueenArviointi
import fi.elsapalvelu.elsa.repository.OsaalueenArviointiRepository
import fi.elsapalvelu.elsa.service.OsaalueenArviointiService
import fi.elsapalvelu.elsa.service.dto.OsaalueenArviointiDTO
import fi.elsapalvelu.elsa.service.mapper.OsaalueenArviointiMapper
import java.util.Optional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service Implementation for managing [OsaalueenArviointi].
 */
@Service
@Transactional
class OsaalueenArviointiServiceImpl(
    private val osaalueenArviointiRepository: OsaalueenArviointiRepository,
    private val osaalueenArviointiMapper: OsaalueenArviointiMapper
) : OsaalueenArviointiService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(osaalueenArviointiDTO: OsaalueenArviointiDTO): OsaalueenArviointiDTO {
        log.debug("Request to save OsaalueenArviointi : $osaalueenArviointiDTO")

        var osaalueenArviointi = osaalueenArviointiMapper.toEntity(osaalueenArviointiDTO)
        osaalueenArviointi = osaalueenArviointiRepository.save(osaalueenArviointi)
        return osaalueenArviointiMapper.toDto(osaalueenArviointi)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<OsaalueenArviointiDTO> {
        log.debug("Request to get all OsaalueenArviointis")
        return osaalueenArviointiRepository.findAll()
            .mapTo(mutableListOf(), osaalueenArviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<OsaalueenArviointiDTO> {
        log.debug("Request to get OsaalueenArviointi : $id")
        return osaalueenArviointiRepository.findById(id)
            .map(osaalueenArviointiMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete OsaalueenArviointi : $id")

        osaalueenArviointiRepository.deleteById(id)
    }
}

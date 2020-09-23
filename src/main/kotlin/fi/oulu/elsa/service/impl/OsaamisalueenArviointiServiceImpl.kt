package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.OsaamisalueenArviointi
import fi.oulu.elsa.repository.OsaamisalueenArviointiRepository
import fi.oulu.elsa.service.OsaamisalueenArviointiService
import fi.oulu.elsa.service.dto.OsaamisalueenArviointiDTO
import fi.oulu.elsa.service.mapper.OsaamisalueenArviointiMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [OsaamisalueenArviointi].
 */
@Service
@Transactional
class OsaamisalueenArviointiServiceImpl(
    private val osaamisalueenArviointiRepository: OsaamisalueenArviointiRepository,
    private val osaamisalueenArviointiMapper: OsaamisalueenArviointiMapper
) : OsaamisalueenArviointiService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(osaamisalueenArviointiDTO: OsaamisalueenArviointiDTO): OsaamisalueenArviointiDTO {
        log.debug("Request to save OsaamisalueenArviointi : $osaamisalueenArviointiDTO")

        var osaamisalueenArviointi = osaamisalueenArviointiMapper.toEntity(osaamisalueenArviointiDTO)
        osaamisalueenArviointi = osaamisalueenArviointiRepository.save(osaamisalueenArviointi)
        return osaamisalueenArviointiMapper.toDto(osaamisalueenArviointi)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<OsaamisalueenArviointiDTO> {
        log.debug("Request to get all OsaamisalueenArviointis")
        return osaamisalueenArviointiRepository.findAll()
            .mapTo(mutableListOf(), osaamisalueenArviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<OsaamisalueenArviointiDTO> {
        log.debug("Request to get OsaamisalueenArviointi : $id")
        return osaamisalueenArviointiRepository.findById(id)
            .map(osaamisalueenArviointiMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete OsaamisalueenArviointi : $id")

        osaamisalueenArviointiRepository.deleteById(id)
    }
}

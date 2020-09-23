package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.OsaamisenArviointi
import fi.oulu.elsa.repository.OsaamisenArviointiRepository
import fi.oulu.elsa.service.OsaamisenArviointiService
import fi.oulu.elsa.service.dto.OsaamisenArviointiDTO
import fi.oulu.elsa.service.mapper.OsaamisenArviointiMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [OsaamisenArviointi].
 */
@Service
@Transactional
class OsaamisenArviointiServiceImpl(
    private val osaamisenArviointiRepository: OsaamisenArviointiRepository,
    private val osaamisenArviointiMapper: OsaamisenArviointiMapper
) : OsaamisenArviointiService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(osaamisenArviointiDTO: OsaamisenArviointiDTO): OsaamisenArviointiDTO {
        log.debug("Request to save OsaamisenArviointi : $osaamisenArviointiDTO")

        var osaamisenArviointi = osaamisenArviointiMapper.toEntity(osaamisenArviointiDTO)
        osaamisenArviointi = osaamisenArviointiRepository.save(osaamisenArviointi)
        return osaamisenArviointiMapper.toDto(osaamisenArviointi)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<OsaamisenArviointiDTO> {
        log.debug("Request to get all OsaamisenArviointis")
        return osaamisenArviointiRepository.findAll()
            .mapTo(mutableListOf(), osaamisenArviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<OsaamisenArviointiDTO> {
        log.debug("Request to get OsaamisenArviointi : $id")
        return osaamisenArviointiRepository.findById(id)
            .map(osaamisenArviointiMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete OsaamisenArviointi : $id")

        osaamisenArviointiRepository.deleteById(id)
    }
}

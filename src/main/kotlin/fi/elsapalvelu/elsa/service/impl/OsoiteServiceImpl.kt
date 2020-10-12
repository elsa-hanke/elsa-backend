package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Osoite
import fi.elsapalvelu.elsa.repository.OsoiteRepository
import fi.elsapalvelu.elsa.service.OsoiteService
import fi.elsapalvelu.elsa.service.dto.OsoiteDTO
import fi.elsapalvelu.elsa.service.mapper.OsoiteMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Osoite].
 */
@Service
@Transactional
class OsoiteServiceImpl(
    private val osoiteRepository: OsoiteRepository,
    private val osoiteMapper: OsoiteMapper
) : OsoiteService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(osoiteDTO: OsoiteDTO): OsoiteDTO {
        log.debug("Request to save Osoite : $osoiteDTO")

        var osoite = osoiteMapper.toEntity(osoiteDTO)
        osoite = osoiteRepository.save(osoite)
        return osoiteMapper.toDto(osoite)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<OsoiteDTO> {
        log.debug("Request to get all Osoites")
        return osoiteRepository.findAll()
            .mapTo(mutableListOf(), osoiteMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<OsoiteDTO> {
        log.debug("Request to get Osoite : $id")
        return osoiteRepository.findById(id)
            .map(osoiteMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Osoite : $id")

        osoiteRepository.deleteById(id)
    }
}

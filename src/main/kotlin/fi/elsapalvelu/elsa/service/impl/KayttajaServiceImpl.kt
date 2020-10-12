package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Kayttaja].
 */
@Service
@Transactional
class KayttajaServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KayttajaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO {
        log.debug("Request to save Kayttaja : $kayttajaDTO")

        var kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
        kayttaja = kayttajaRepository.save(kayttaja)
        return kayttajaMapper.toDto(kayttaja)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<KayttajaDTO> {
        log.debug("Request to get all Kayttajas")
        return kayttajaRepository.findAll()
            .mapTo(mutableListOf(), kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KayttajaDTO> {
        log.debug("Request to get Kayttaja : $id")
        return kayttajaRepository.findById(id)
            .map(kayttajaMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Kayttaja : $id")

        kayttajaRepository.deleteById(id)
    }
}

package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.service.mapper.KouluttajavaltuutusMapper
import java.util.Optional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KouluttajavaltuutusServiceImpl(
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val kouluttajavaltuutusMapper: KouluttajavaltuutusMapper
) : KouluttajavaltuutusService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(kouluttajavaltuutusDTO: KouluttajavaltuutusDTO): KouluttajavaltuutusDTO {
        log.debug("Request to save Kouluttajavaltuutus : $kouluttajavaltuutusDTO")

        var kouluttajavaltuutus = kouluttajavaltuutusMapper.toEntity(kouluttajavaltuutusDTO)
        kouluttajavaltuutus = kouluttajavaltuutusRepository.save(kouluttajavaltuutus)
        return kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<KouluttajavaltuutusDTO> {
        log.debug("Request to get all Kouluttajavaltuutuses")
        return kouluttajavaltuutusRepository.findAll()
            .mapTo(mutableListOf(), kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KouluttajavaltuutusDTO> {
        log.debug("Request to get Kouluttajavaltuutus : $id")
        return kouluttajavaltuutusRepository.findById(id)
            .map(kouluttajavaltuutusMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Kouluttajavaltuutus : $id")

        kouluttajavaltuutusRepository.deleteById(id)
    }
}

package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KouluttajavaltuutusMapper
import java.util.Optional
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KouluttajavaltuutusServiceImpl(
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val kouluttajavaltuutusMapper: KouluttajavaltuutusMapper,
    private val kayttajaMapper: KayttajaMapper
) : KouluttajavaltuutusService {

    override fun save(kouluttajavaltuutusDTO: KouluttajavaltuutusDTO): KouluttajavaltuutusDTO {
        var kouluttajavaltuutus = kouluttajavaltuutusMapper.toEntity(kouluttajavaltuutusDTO)
        kouluttajavaltuutus = kouluttajavaltuutusRepository.save(kouluttajavaltuutus)
        return kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findAll()
            .mapTo(mutableListOf(), kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllValtuutettuByValtuuttajaKayttajaUserId(id: String): MutableList<KayttajaDTO> {
        return kouluttajavaltuutusRepository.findAllByValtuuttajaKayttajaUserId(id).map { it.valtuutettu!! }
            .mapTo(mutableListOf(), kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findById(id)
            .map(kouluttajavaltuutusMapper::toDto)
    }

    override fun delete(id: Long) {
        kouluttajavaltuutusRepository.deleteById(id)
    }
}

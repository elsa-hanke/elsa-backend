package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class KayttajaServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val kayttajaMapper: KayttajaMapper
) : KayttajaService {

    override fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO {
        var kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
        kayttaja = kayttajaRepository.save(kayttaja)
        return kayttajaMapper.toDto(kayttaja)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<KayttajaDTO> {
        return kayttajaRepository.findAll()
            .mapTo(mutableListOf(), kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KayttajaDTO> {
        return kayttajaRepository.findById(id)
            .map(kayttajaMapper::toDto)
    }

    override fun delete(id: Long) {
        kayttajaRepository.deleteById(id)
    }
}

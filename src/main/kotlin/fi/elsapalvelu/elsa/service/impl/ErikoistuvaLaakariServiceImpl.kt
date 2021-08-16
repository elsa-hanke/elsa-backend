package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.dto.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoistuvaLaakariMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class ErikoistuvaLaakariServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val erikoistuvaLaakariMapper: ErikoistuvaLaakariMapper
) : ErikoistuvaLaakariService {

    override fun save(erikoistuvaLaakariDTO: ErikoistuvaLaakariDTO): ErikoistuvaLaakariDTO {
        var erikoistuvaLaakari = erikoistuvaLaakariMapper.toEntity(erikoistuvaLaakariDTO)
        erikoistuvaLaakari = erikoistuvaLaakariRepository.save(erikoistuvaLaakari)
        return erikoistuvaLaakariMapper.toDto(erikoistuvaLaakari)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ErikoistuvaLaakariDTO> {
        return erikoistuvaLaakariRepository.findAll()
            .map(erikoistuvaLaakariMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ErikoistuvaLaakariDTO> {
        return erikoistuvaLaakariRepository.findById(id)
            .map(erikoistuvaLaakariMapper::toDto)
    }

    override fun delete(id: Long) {
        erikoistuvaLaakariRepository.deleteById(id)
    }

    override fun findOneByKayttajaUserId(
        id: String
    ): ErikoistuvaLaakariDTO? {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(id)?.let {
            return erikoistuvaLaakariMapper.toDto(it)
        }

        return null
    }
}

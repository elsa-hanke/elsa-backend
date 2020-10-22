package fi.elsapalvelu.elsa.service
import fi.elsapalvelu.elsa.repository.SoteOrganisaatioRepository
import fi.elsapalvelu.elsa.service.dto.SoteOrganisaatioDTO
import fi.elsapalvelu.elsa.service.mapper.SoteOrganisaatioMapper
import java.util.Optional
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SoteOrganisaatioService(
    private val soteOrganisaatioRepository: SoteOrganisaatioRepository,
    private val soteOrganisaatioMapper: SoteOrganisaatioMapper
) {
    fun save(soteOrganisaatioDTO: SoteOrganisaatioDTO): SoteOrganisaatioDTO {
        var soteOrganisaatio = soteOrganisaatioMapper.toEntity(soteOrganisaatioDTO)
        soteOrganisaatio = soteOrganisaatioRepository.save(soteOrganisaatio)
        return soteOrganisaatioMapper.toDto(soteOrganisaatio)
    }

    @Transactional(readOnly = true)
    fun findAll(): MutableList<SoteOrganisaatioDTO> {
        return soteOrganisaatioRepository.findAll()
            .mapTo(mutableListOf(), soteOrganisaatioMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun findOne(id: String): Optional<SoteOrganisaatioDTO> {
        return soteOrganisaatioRepository.findById(id)
            .map(soteOrganisaatioMapper::toDto)
    }

    fun delete(id: String) {
        soteOrganisaatioRepository.deleteById(id)
    }
}

package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.service.SuoritemerkintaService
import fi.elsapalvelu.elsa.service.dto.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritemerkintaMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
@Transactional
class SuoritemerkintaServiceImpl(
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val suoritemerkintaMapper: SuoritemerkintaMapper
) : SuoritemerkintaService {

    override fun save(suoritemerkintaDTO: SuoritemerkintaDTO): SuoritemerkintaDTO {
        var suoritemerkinta = suoritemerkintaMapper.toEntity(suoritemerkintaDTO)
        suoritemerkinta = suoritemerkintaRepository.save(suoritemerkinta)
        return suoritemerkintaMapper.toDto(suoritemerkinta)
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findAll(pageable)
            .map(suoritemerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: String
    ): MutableList<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(id)
            .mapTo(mutableListOf(), suoritemerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<SuoritemerkintaDTO> {
        return suoritemerkintaRepository.findById(id)
            .map(suoritemerkintaMapper::toDto)
    }

    override fun delete(id: Long) {
        suoritemerkintaRepository.deleteById(id)
    }
}

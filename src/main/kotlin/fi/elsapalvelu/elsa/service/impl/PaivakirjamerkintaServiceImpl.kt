package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.service.PaivakirjamerkintaService
import fi.elsapalvelu.elsa.service.dto.PaivakirjamerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.PaivakirjamerkintaMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PaivakirjamerkintaServiceImpl(
    private val paivakirjamerkintaRepository: PaivakirjamerkintaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val paivakirjamerkintaMapper: PaivakirjamerkintaMapper
) : PaivakirjamerkintaService {

    override fun save(paivakirjamerkintaDTO: PaivakirjamerkintaDTO, userId: String): PaivakirjamerkintaDTO? {
        return erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { erikoistuvaLaakari ->
            paivakirjamerkintaDTO.erikoistuvaLaakariId = erikoistuvaLaakari.id
            var paivakirjamerkinta = paivakirjamerkintaMapper.toEntity(paivakirjamerkintaDTO)
            paivakirjamerkinta = paivakirjamerkintaRepository.save(paivakirjamerkinta)
            paivakirjamerkintaMapper.toDto(paivakirjamerkinta)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable, userId: String): Page<PaivakirjamerkintaDTO> {
        return paivakirjamerkintaRepository.findAllByErikoistuvaLaakariKayttajaUserId(pageable, userId)
            .map(paivakirjamerkintaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long, userId: String): PaivakirjamerkintaDTO? {
        return paivakirjamerkintaRepository.findOneByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)?.let {
            paivakirjamerkintaMapper.toDto(it)
        }
    }

    override fun delete(id: Long, userId: String) {
        paivakirjamerkintaRepository.deleteByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)
    }
}

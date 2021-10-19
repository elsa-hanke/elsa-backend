package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.service.TeoriakoulutusService
import fi.elsapalvelu.elsa.service.dto.TeoriakoulutusDTO
import fi.elsapalvelu.elsa.service.mapper.TeoriakoulutusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TeoriakoulutusServiceImpl(
    private val teoriakoulutusRepository: TeoriakoulutusRepository,
    private val teoriakoulutusMapper: TeoriakoulutusMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
) : TeoriakoulutusService {

    override fun save(
        teoriakoulutusDTO: TeoriakoulutusDTO,
        userId: String
    ): TeoriakoulutusDTO? {
        return erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let { erikoistuvaLaakari ->
            teoriakoulutusDTO.erikoistuvaLaakariId = erikoistuvaLaakari.id
            var teoriakoulutus = teoriakoulutusMapper.toEntity(teoriakoulutusDTO)
            teoriakoulutus = teoriakoulutusRepository.save(teoriakoulutus)
            teoriakoulutusMapper.toDto(teoriakoulutus)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(
        userId: String
    ): List<TeoriakoulutusDTO> {
        return teoriakoulutusRepository.findAllByErikoistuvaLaakariKayttajaUserId(userId)
            .map(teoriakoulutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long,
        userId: String
    ): TeoriakoulutusDTO? {
        return teoriakoulutusRepository.findOneByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)?.let {
            teoriakoulutusMapper.toDto(it)
        }
    }

    override fun delete(
        id: Long,
        userId: String
    ) {
        teoriakoulutusRepository.deleteByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)
    }
}

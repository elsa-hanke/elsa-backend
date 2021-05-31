package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.EpaOsaamisalueenKategoriaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.EpaOsaamisalueenKategoriaService
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueenKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.EpaOsaamisalueenKategoriaMapper
import java.util.Optional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EpaOsaamisalueenKategoriaServiceImpl(
    private val epaOsaamisalueenKategoriaRepository: EpaOsaamisalueenKategoriaRepository,
    private val epaOsaamisalueenKategoriaMapper: EpaOsaamisalueenKategoriaMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : EpaOsaamisalueenKategoriaService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(epaOsaamisalueenKategoriaDTO: EpaOsaamisalueenKategoriaDTO): EpaOsaamisalueenKategoriaDTO {
        log.debug("Request to save EpaOsaamisalueenKategoria : $epaOsaamisalueenKategoriaDTO")

        var epaOsaamisalueenKategoria =
            epaOsaamisalueenKategoriaMapper.toEntity(epaOsaamisalueenKategoriaDTO)
        epaOsaamisalueenKategoria =
            epaOsaamisalueenKategoriaRepository.save(epaOsaamisalueenKategoria)
        return epaOsaamisalueenKategoriaMapper.toDto(epaOsaamisalueenKategoria)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<EpaOsaamisalueenKategoriaDTO> {
        log.debug("Request to get all EpaOsaamisalueenKategoriat")

        return epaOsaamisalueenKategoriaRepository.findAll()
            .mapTo(mutableListOf(), epaOsaamisalueenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): MutableList<EpaOsaamisalueenKategoriaDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        return epaOsaamisalueenKategoriaRepository.findAllByEpaOsaamisalueetErikoisalaId(
            kirjautunutErikoistuvaLaakari?.erikoisala?.id
        )
            .mapTo(mutableListOf(), epaOsaamisalueenKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<EpaOsaamisalueenKategoriaDTO> {
        log.debug("Request to get EpaOsaamisalueenKategoria : $id")

        return epaOsaamisalueenKategoriaRepository.findById(id)
            .map(epaOsaamisalueenKategoriaMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete EpaOsaamisalueenKategoria : $id")

        epaOsaamisalueenKategoriaRepository.deleteById(id)
    }
}

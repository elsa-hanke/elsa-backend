package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.EpaOsaamisalueRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.EpaOsaamisalueService
import fi.elsapalvelu.elsa.service.dto.EpaOsaamisalueDTO
import fi.elsapalvelu.elsa.service.mapper.EpaOsaamisalueMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class EpaOsaamisalueServiceImpl(
    private val epaOsaamisalueRepository: EpaOsaamisalueRepository,
    private val epaOsaamisalueMapper: EpaOsaamisalueMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : EpaOsaamisalueService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(epaOsaamisalueDTO: EpaOsaamisalueDTO): EpaOsaamisalueDTO {
        log.debug("Request to save EpaOsaamisalue : $epaOsaamisalueDTO")

        var epaOsaamisalue = epaOsaamisalueMapper.toEntity(epaOsaamisalueDTO)
        epaOsaamisalue = epaOsaamisalueRepository.save(epaOsaamisalue)
        return epaOsaamisalueMapper.toDto(epaOsaamisalue)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<EpaOsaamisalueDTO> {
        log.debug("Request to get all EpaOsaamisalueet")

        return epaOsaamisalueRepository.findAll()
            .map(epaOsaamisalueMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaId(kayttajaId: String): List<EpaOsaamisalueDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaId(kayttajaId)
        // Jos erikoistumisen aloituspäivää ei ole määritetty, käytetään nykyistä päivää voimassaolon rajaamisessa
        return epaOsaamisalueRepository.findAllByErikoisalaIdAndValid(
            kirjautunutErikoistuvaLaakari?.erikoisala?.id,
            kirjautunutErikoistuvaLaakari?.erikoistumisenAloituspaiva ?: LocalDate.now()
        )
            .map(epaOsaamisalueMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<EpaOsaamisalueDTO> {
        log.debug("Request to get EpaOsaamisalue : $id")

        return epaOsaamisalueRepository.findById(id)
            .map(epaOsaamisalueMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete EpaOsaamisalue : $id")

        epaOsaamisalueRepository.deleteById(id)
    }
}

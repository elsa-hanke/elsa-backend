package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.ErikoisalaService
import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class ErikoisalaServiceImpl(
    private val erikoisalaRepository: ErikoisalaRepository,
    private val erikoisalaMapper: ErikoisalaMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : ErikoisalaService {

    override fun save(erikoisalaDTO: ErikoisalaDTO): ErikoisalaDTO {
        var erikoisala = erikoisalaMapper.toEntity(erikoisalaDTO)
        erikoisala = erikoisalaRepository.save(erikoisala)
        return erikoisalaMapper.toDto(erikoisala)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ErikoisalaDTO> {
        return erikoisalaRepository.findAll()
            .map(erikoisalaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<ErikoisalaDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        val opiskeluoikeus = kirjautunutErikoistuvaLaakari?.opiskeluoikeudet?.firstOrNull()

        // Jos päivämäärää jonka mukainen opintosuunnitelma käytössä ei ole määritetty, käytetään nykyistä päivää
        // voimassaolon rajaamisessa
        return erikoisalaRepository.findAllByValid(
            opiskeluoikeus?.opintosuunnitelmaKaytossaPvm ?: LocalDate.now()
        )
            .map(erikoisalaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ErikoisalaDTO> {
        return erikoisalaRepository.findById(id)
            .map(erikoisalaMapper::toDto)
    }

    override fun delete(id: Long) {
        erikoisalaRepository.deleteById(id)
    }
}

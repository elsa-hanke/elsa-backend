package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArvioitavaKokonaisuusRepository
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.ArvioitavaKokonaisuusService
import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusDTO
import fi.elsapalvelu.elsa.service.mapper.ArvioitavaKokonaisuusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class ArvioitavaKokonaisuusServiceImpl(
    private val arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository,
    private val arvioitavaKokonaisuusMapper: ArvioitavaKokonaisuusMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : ArvioitavaKokonaisuusService {

    override fun save(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO {
        var arvioitavaKokonaisuus = arvioitavaKokonaisuusMapper.toEntity(arvioitavaKokonaisuusDTO)
        arvioitavaKokonaisuus = arvioitavaKokonaisuusRepository.save(arvioitavaKokonaisuus)
        return arvioitavaKokonaisuusMapper.toDto(arvioitavaKokonaisuus)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<ArvioitavaKokonaisuusDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        // Jos päivämäärää jonka mukainen opintosuunnitelma käytössä, ei ole määritetty, käytetään nykyistä päivää
        // voimassaolon rajaamisessa
        return arvioitavaKokonaisuusRepository.findAllByErikoisalaIdAndValid(
            kirjautunutErikoistuvaLaakari?.erikoisala?.id,
            kirjautunutErikoistuvaLaakari?.opintosuunnitelmaKaytossaPvm ?: LocalDate.now()
        )
            .map(arvioitavaKokonaisuusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArvioitavaKokonaisuusDTO> {
        return arvioitavaKokonaisuusRepository.findById(id)
            .map(arvioitavaKokonaisuusMapper::toDto)
    }

    override fun delete(id: Long) {
        arvioitavaKokonaisuusRepository.deleteById(id)
    }
}

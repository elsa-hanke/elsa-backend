package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArvioitavaKokonaisuusRepository
import fi.elsapalvelu.elsa.repository.OpintooikeusRepository
import fi.elsapalvelu.elsa.service.ArvioitavaKokonaisuusService
import fi.elsapalvelu.elsa.service.dto.ArvioitavaKokonaisuusDTO
import fi.elsapalvelu.elsa.service.mapper.ArvioitavaKokonaisuusMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class ArvioitavaKokonaisuusServiceImpl(
    private val arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository,
    private val arvioitavaKokonaisuusMapper: ArvioitavaKokonaisuusMapper,
    private val opintooikeusRepository: OpintooikeusRepository
) : ArvioitavaKokonaisuusService {

    override fun save(arvioitavaKokonaisuusDTO: ArvioitavaKokonaisuusDTO): ArvioitavaKokonaisuusDTO {
        var arvioitavaKokonaisuus = arvioitavaKokonaisuusMapper.toEntity(arvioitavaKokonaisuusDTO)
        arvioitavaKokonaisuus = arvioitavaKokonaisuusRepository.save(arvioitavaKokonaisuus)
        return arvioitavaKokonaisuusMapper.toDto(arvioitavaKokonaisuus)
    }

    @Transactional(readOnly = true)
    override fun findAllByOpintooikeusId(opintooikeusId: Long): List<ArvioitavaKokonaisuusDTO> {
        return opintooikeusRepository.findByIdOrNull(opintooikeusId)?.let {
            // Jos päivämäärää jonka mukainen opintosuunnitelma käytössä, ei ole määritetty, käytetään nykyistä päivää
            // voimassaolon rajaamisessa
            arvioitavaKokonaisuusRepository.findAllByErikoisalaIdAndValid(
                it.erikoisala?.id, it.osaamisenArvioinninOppaanPvm ?: LocalDate.now()
            ).map(arvioitavaKokonaisuusMapper::toDto)
        } ?: listOf()
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

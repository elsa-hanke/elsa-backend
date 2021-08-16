package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.service.KouluttajavaltuutusService
import fi.elsapalvelu.elsa.service.dto.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KouluttajavaltuutusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
@Transactional
class KouluttajavaltuutusServiceImpl(
    private val kouluttajavaltuutusRepository: KouluttajavaltuutusRepository,
    private val kouluttajavaltuutusMapper: KouluttajavaltuutusMapper,
    private val kayttajaMapper: KayttajaMapper
) : KouluttajavaltuutusService {

    override fun save(kouluttajavaltuutusDTO: KouluttajavaltuutusDTO): KouluttajavaltuutusDTO {
        var kouluttajavaltuutus = kouluttajavaltuutusMapper.toEntity(kouluttajavaltuutusDTO)
        kouluttajavaltuutus = kouluttajavaltuutusRepository.save(kouluttajavaltuutus)
        // TODO: lähetä sähköposti kouluttajalle
        return kouluttajavaltuutusMapper.toDto(kouluttajavaltuutus)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findAll()
            .map(kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findAllValtuutettuByValtuuttajaKayttajaUserId(id: String): List<KayttajaDTO> {
        return kouluttajavaltuutusRepository.findAllByValtuuttajaKayttajaUserIdAndPaattymispaivaAfter(
            id,
            LocalDate.now()
        ).map { it.valtuutettu!! }
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findValtuutettuByValtuuttajaAndValtuutettu(
        valtuutettuId: String,
        valtuuttajaId: String
    ): Optional<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findByValtuuttajaKayttajaUserIdAndValtuutettuUserIdAndPaattymispaivaAfter(
            valtuutettuId,
            valtuuttajaId,
            LocalDate.now()
        )
            .map(kouluttajavaltuutusMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KouluttajavaltuutusDTO> {
        return kouluttajavaltuutusRepository.findById(id)
            .map(kouluttajavaltuutusMapper::toDto)
    }

    override fun delete(id: Long) {
        kouluttajavaltuutusRepository.deleteById(id)
    }
}

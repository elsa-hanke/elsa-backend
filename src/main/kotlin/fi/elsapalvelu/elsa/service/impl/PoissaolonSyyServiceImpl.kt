package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.PoissaolonSyyRepository
import fi.elsapalvelu.elsa.service.PoissaolonSyyService
import fi.elsapalvelu.elsa.service.dto.PoissaolonSyyDTO
import fi.elsapalvelu.elsa.service.mapper.PoissaolonSyyMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class PoissaolonSyyServiceImpl(
    private val poissaolonSyyRepository: PoissaolonSyyRepository,
    private val poissaolonSyyMapper: PoissaolonSyyMapper,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
) : PoissaolonSyyService {

    override fun save(poissaolonSyyDTO: PoissaolonSyyDTO): PoissaolonSyyDTO {
        var poissaolonSyy = poissaolonSyyMapper.toEntity(poissaolonSyyDTO)
        poissaolonSyy = poissaolonSyyRepository.save(poissaolonSyy)
        return poissaolonSyyMapper.toDto(poissaolonSyy)
    }

    @Transactional(readOnly = true)
    override fun findAllByErikoistuvaLaakariKayttajaUserId(userId: String): List<PoissaolonSyyDTO> {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        // Jos päivämäärää jonka mukainen opintosuunnitelma käytössä ei ole määritetty, käytetään nykyistä päivää
        // voimassaolon rajaamisessa
        return poissaolonSyyRepository.findAllByValid(
            kirjautunutErikoistuvaLaakari?.opintosuunnitelmaKaytossaPvm ?: LocalDate.now())
            .map(poissaolonSyyMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): PoissaolonSyyDTO? {
        poissaolonSyyRepository.findByIdOrNull(id)?.let {
            return poissaolonSyyMapper.toDto(it)
        }
        return null
    }

    override fun delete(id: Long) {
        poissaolonSyyRepository.deleteById(id)
    }
}
